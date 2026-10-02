package com.taskflow.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Local multi-language code runner used by the TaskFlow DSA agent.
 *
 * This runner supports Java, Python, JavaScript, C, and C++. It compiles
 * submitted
 * code when needed and runs it as a separate operating-system process. The
 * Spring Boot
 * process itself never evals/interprets submitted source code.
 *
 * IMPORTANT: this is suitable for a controlled local/demo environment, not for
 * an open public judge. OS-process isolation is NOT a security sandbox. For a
 * public multi-user deployment use a real sandbox/container runner.
 */
@Service
public class CodeExecutionService {

    @Value("${code.local.java-command:java}")
    private String javaCommand;

    @Value("${code.local.javac-command:javac}")
    private String javacCommand;

    @Value("${code.local.python-command:python}")
    private String pythonCommand;

    @Value("${code.local.node-command:node}")
    private String nodeCommand;

    @Value("${code.local.c-command:gcc}")
    private String cCommand;

    @Value("${code.local.cpp-command:g++}")
    private String cppCommand;

    @Value("${code.local.compile-timeout-seconds:10}")
    private long compileTimeoutSeconds;

    @Value("${code.local.run-timeout-seconds:5}")
    private long runTimeoutSeconds;

    @Value("${code.local.max-output-chars:20000}")
    private int maxOutputChars;

    public Result run(String language, String code, String stdin) {
        if (code == null || code.isBlank()) {
            return new Result("", "Source code is empty.", -1);
        }
        String selectedLanguage = normalizeLanguage(language);
        if (selectedLanguage == null) {
            return new Result("", "Unsupported language. Choose Java, Python, JavaScript, C, or C++.", -1);
        }
        if (selectedLanguage.equals("java") && !containsMainClass(code)) {
            return new Result("", "Java solution must contain 'public class Main' with a main method.", -1);
        }

        Path dir = null;
        try {
            dir = Files.createTempDirectory("taskflow-code-");
            String sourceName = sourceName(selectedLanguage);
            Path source = dir.resolve(sourceName);
            Files.writeString(source, code, StandardCharsets.UTF_8);

            if (requiresCompilation(selectedLanguage)) {
                ProcessResult compile = execute(
                        compileCommand(selectedLanguage, dir, sourceName),
                        "",
                        Duration.ofSeconds(Math.max(1, compileTimeoutSeconds)));
                if (compile.timedOut) {
                    return new Result("", "Compilation timed out.", -1);
                }
                if (compile.exitCode != 0) {
                    return new Result("", "Compilation error: " + firstNonBlank(compile.stderr, compile.stdout), -1);
                }
            }

            ProcessBuilder runBuilder = runCommand(selectedLanguage, dir).directory(dir.toFile());

            ProcessResult run = execute(
                    runBuilder,
                    stdin == null ? "" : stdin,
                    Duration.ofSeconds(Math.max(1, runTimeoutSeconds)));

            if (run.timedOut) {
                return new Result(limit(run.stdout), "Execution timed out after " + runTimeoutSeconds + " seconds.",
                        -1);
            }

            return new Result(limit(run.stdout), limit(run.stderr), run.exitCode);
        } catch (IOException e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (msg.toLowerCase(Locale.ROOT).contains("cannot run program")
                    || msg.toLowerCase(Locale.ROOT).contains("createprocess")) {
                msg = "Java compiler/runtime was not found. Make sure JDK 17+ is installed and both 'java' and 'javac' work in the terminal. Details: "
                        + msg;
            }
            return new Result("", msg, -1);
        } catch (Exception e) {
            return new Result("",
                    "Local runner failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()),
                    -1);
        } finally {
            deleteRecursively(dir);
        }
    }

    private String normalizeLanguage(String language) {
        String value = language == null || language.isBlank() ? "java" : language.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "java" -> "java";
            case "python", "py" -> "python";
            case "javascript", "js", "node", "nodejs" -> "javascript";
            case "c" -> "c";
            case "cpp", "c++" -> "cpp";
            default -> null;
        };
    }

    private String sourceName(String language) {
        return switch (language) {
            case "java" -> "Main.java";
            case "python" -> "Main.py";
            case "javascript" -> "Main.js";
            case "c" -> "Main.c";
            case "cpp" -> "Main.cpp";
            default -> throw new IllegalArgumentException("Unsupported language");
        };
    }

    private boolean requiresCompilation(String language) {
        return language.equals("java") || language.equals("c") || language.equals("cpp");
    }

    private ProcessBuilder compileCommand(String language, Path dir, String sourceName) {
        return switch (language) {
            case "java" -> new ProcessBuilder(javacCommand, "-encoding", "UTF-8", sourceName).directory(dir.toFile());
            case "c" -> new ProcessBuilder(cCommand, sourceName, "-o", executablePath(dir)).directory(dir.toFile());
            case "cpp" -> new ProcessBuilder(cppCommand, "-std=c++17", sourceName, "-o", executablePath(dir))
                    .directory(dir.toFile());
            default -> throw new IllegalArgumentException("Unsupported compiled language");
        };
    }

    private ProcessBuilder runCommand(String language, Path dir) {
        return switch (language) {
            case "java" -> new ProcessBuilder(javaCommand, "-Xms16m", "-Xmx128m", "-XX:ActiveProcessorCount=1", "-cp",
                    dir.toAbsolutePath().toString(), "Main");
            case "python" -> new ProcessBuilder(pythonCommand, "Main.py");
            case "javascript" -> new ProcessBuilder(nodeCommand, "Main.js");
            case "c", "cpp" -> new ProcessBuilder(executablePath(dir));
            default -> throw new IllegalArgumentException("Unsupported language");
        };
    }

    private String executablePath(Path dir) {
        return dir.resolve(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "Main.exe" : "Main")
                .toAbsolutePath().toString();
    }

    private ProcessResult execute(ProcessBuilder builder, String stdin, Duration timeout) throws Exception {
        Process process = builder.start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            if (stdin != null && !stdin.isEmpty())
                writer.write(stdin);
            writer.flush();
        }

        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
            return new ProcessResult(limit(stdoutFuture.getNow("")), limit(stderrFuture.getNow("")), -1, true);
        }

        String stdout = stdoutFuture.get(2, TimeUnit.SECONDS);
        String stderr = stderrFuture.get(2, TimeUnit.SECONDS);
        return new ProcessResult(limit(stdout), limit(stderr), process.exitValue(), false);
    }

    private String read(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            char[] buffer = new char[2048];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                int room = Math.max(0, maxOutputChars - out.length());
                if (room <= 0)
                    break;
                out.append(buffer, 0, Math.min(n, room));
            }
            return out.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private boolean containsMainClass(String code) {
        return code.matches("(?s).*public\\s+class\\s+Main\\b.*")
                && code.matches("(?s).*static\\s+void\\s+main\\s*\\(.*");
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank())
            return limit(a.trim());
        if (b != null && !b.isBlank())
            return limit(b.trim());
        return "Unknown compilation error";
    }

    private String limit(String value) {
        if (value == null)
            return "";
        if (value.length() <= maxOutputChars)
            return value;
        return value.substring(0, maxOutputChars) + "\n[output truncated]";
    }

    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path))
            return;
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private record ProcessResult(String stdout, String stderr, int exitCode, boolean timedOut) {
    }

    public record Result(String stdout, String stderr, int exitCode) {
    }
}
