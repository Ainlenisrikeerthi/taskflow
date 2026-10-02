package com.taskflow.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.dto.coding.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CodingAgentService {
    @Value("${ai.openrouter.api-key:}") private String openRouterApiKey;
    @Value("${ai.openrouter.primary-model:}") private String openRouterPrimaryModel;
    @Value("${ai.openrouter.fallback-model:}") private String openRouterFallbackModel;
    @Value("${ai.openrouter.site-url:}") private String openRouterSiteUrl;
    @Value("${ai.openrouter.app-name:TaskFlow}") private String openRouterAppName;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeneratedCodingTaskResponse generate(GenerateCodingTaskRequest r) {
        String title = (r.title == null || r.title.isBlank()) ? "Coding Challenge" : r.title.trim();
        String difficulty = (r.difficulty == null || r.difficulty.isBlank()) ? "MEDIUM" : r.difficulty.toUpperCase();
        String language = (r.language == null || r.language.isBlank()) ? "java" : r.language;

        if (isOpenRouterConfigured()) {
            try {
                GeneratedCodingTaskResponse ai = generateWithOpenRouter(title, difficulty, language);
                if (ai != null && ai.description != null && !ai.description.isBlank() && ai.testCases != null && !ai.testCases.isEmpty()) {
                    return ai;
                }
            } catch (Exception ignored) {
                // Fall back to deterministic generator so the admin can still work.
            }
        }
        return generateFallback(title, difficulty, language);
    }

    private GeneratedCodingTaskResponse generateWithOpenRouter(String title, String difficulty, String language) throws Exception {
        String prompt = """
            You are a DSA assessment author. Create a complete coding problem for an admin to review before publishing.
            Return ONLY valid JSON with this exact shape:
            {
              \"description\": \"clear problem statement with input/output format and examples\",
              \"instructions\": \"constraints and important notes\",
              \"starterCode\": \"starter code for the requested language\",
              \"testCases\": [
                {\"input\":\"...\",\"expectedOutput\":\"...\",\"hidden\":false},
                {\"input\":\"...\",\"expectedOutput\":\"...\",\"hidden\":true}
              ]
            }
            Generate at least 5 test cases: 2 visible and 3 hidden, including normal, boundary, duplicate/edge, and larger cases where relevant.
            Do not include markdown fences.
            Title: %s
            Difficulty: %s
            Language: %s
            """.formatted(title, difficulty, language);

        String text = callOpenRouterForText(prompt);
        text = text.replace("```json", "").replace("```", "").trim();
        JsonNode json = mapper.readTree(text);

        GeneratedCodingTaskResponse out = new GeneratedCodingTaskResponse();
        out.description = json.path("description").asText();
        out.instructions = json.path("instructions").asText();
        out.starterCode = json.path("starterCode").asText();
        for (JsonNode tc : json.path("testCases")) {
            out.testCases.add(new TestCaseDto(null, tc.path("input").asText(), tc.path("expectedOutput").asText(), tc.path("hidden").asBoolean()));
        }
        return out;
    }

    private GeneratedCodingTaskResponse generateFallback(String title, String difficulty, String language) {
        String key = title.toLowerCase(Locale.ROOT);
        GeneratedCodingTaskResponse x = new GeneratedCodingTaskResponse();

        if (key.contains("two sum")) {
            x.description = "Given an array of integers and a target, print the zero-based indices of two distinct elements whose sum equals the target.\n\nInput format:\nFirst line: n\nSecond line: n space-separated integers\nThird line: target\n\nOutput format:\nPrint the two indices in increasing order separated by one space.";
            x.instructions = "Difficulty: " + difficulty + ". Exactly one valid pair exists. Aim for O(n) time using a hash-based approach. Admin should review all generated cases before publishing.";
            x.testCases.add(new TestCaseDto(null,"4\n2 7 11 15\n9","0 1",false));
            x.testCases.add(new TestCaseDto(null,"3\n3 2 4\n6","1 2",false));
            x.testCases.add(new TestCaseDto(null,"2\n3 3\n6","0 1",true));
            x.testCases.add(new TestCaseDto(null,"5\n-3 4 3 90 1\n0","0 2",true));
            x.testCases.add(new TestCaseDto(null,"6\n1 5 8 2 9 4\n13","1 2",true));
        } else if (key.contains("palindrome")) {
            x.description = "Read a single string and print true if it reads the same forward and backward; otherwise print false. Comparison is case-sensitive unless the admin changes this requirement.";
            x.instructions = "Difficulty: " + difficulty + ". Handle empty/single-character style edge cases according to the input constraints. Prefer O(n) time and O(1) extra space when possible.";
            x.testCases.add(new TestCaseDto(null,"racecar","true",false));
            x.testCases.add(new TestCaseDto(null,"hello","false",false));
            x.testCases.add(new TestCaseDto(null,"a","true",true));
            x.testCases.add(new TestCaseDto(null,"abba","true",true));
            x.testCases.add(new TestCaseDto(null,"abca","false",true));
        } else if (key.contains("factorial")) {
            x.description = "Read a non-negative integer n and print n factorial.";
            x.instructions = "Difficulty: " + difficulty + ". 0! = 1. Admin should set a numeric range appropriate for the chosen language.";
            x.testCases.add(new TestCaseDto(null,"5","120",false));
            x.testCases.add(new TestCaseDto(null,"0","1",false));
            x.testCases.add(new TestCaseDto(null,"1","1",true));
            x.testCases.add(new TestCaseDto(null,"6","720",true));
            x.testCases.add(new TestCaseDto(null,"10","3628800",true));
        } else if (key.contains("fibonacci")) {
            x.description = "Read n and print the nth Fibonacci number using F(0)=0 and F(1)=1.";
            x.instructions = "Difficulty: " + difficulty + ". Avoid exponential recursion for larger n. Prefer O(n) time and O(1) auxiliary space.";
            x.testCases.add(new TestCaseDto(null,"7","13",false));
            x.testCases.add(new TestCaseDto(null,"0","0",false));
            x.testCases.add(new TestCaseDto(null,"1","1",true));
            x.testCases.add(new TestCaseDto(null,"10","55",true));
            x.testCases.add(new TestCaseDto(null,"20","6765",true));
        } else if (key.contains("reverse") && key.contains("string")) {
            x.description = "Read one line of text and print the characters in reverse order.";
            x.instructions = "Difficulty: " + difficulty + ". Preserve spaces and character case.";
            x.testCases.add(new TestCaseDto(null,"hello","olleh",false));
            x.testCases.add(new TestCaseDto(null,"TaskFlow","wolFksaT",false));
            x.testCases.add(new TestCaseDto(null,"a","a",true));
            x.testCases.add(new TestCaseDto(null,"ab cd","dc ba",true));
            x.testCases.add(new TestCaseDto(null,"12345","54321",true));
        } else {
            x.description = "Solve “" + title + "”. Read the input from standard input and print only the required output. This fallback draft was generated without an external AI model, so the admin must edit the exact input/output specification before publishing.";
            x.instructions = "Difficulty: " + difficulty + ". Configure OPENROUTER_API_KEY and OPENROUTER_PRIMARY_MODEL to enable full AI-generated arbitrary DSA descriptions and test cases. The final score considers correctness, efficiency and code quality.";
            x.testCases.add(new TestCaseDto(null,"EDIT_VISIBLE_INPUT_1","EDIT_EXPECTED_OUTPUT_1",false));
            x.testCases.add(new TestCaseDto(null,"EDIT_VISIBLE_INPUT_2","EDIT_EXPECTED_OUTPUT_2",false));
            x.testCases.add(new TestCaseDto(null,"EDIT_HIDDEN_INPUT_1","EDIT_EXPECTED_OUTPUT_1",true));
            x.testCases.add(new TestCaseDto(null,"EDIT_HIDDEN_INPUT_2","EDIT_EXPECTED_OUTPUT_2",true));
            x.testCases.add(new TestCaseDto(null,"EDIT_HIDDEN_INPUT_3","EDIT_EXPECTED_OUTPUT_3",true));
        }

        x.starterCode = starter(language);
        return x;
    }

    private String starter(String language) {
        return switch (language.toLowerCase(Locale.ROOT)) {
            case "python" -> "# Read from stdin and print the answer\n";
            case "javascript", "js" -> "// Read from stdin and print the answer\n";
            case "cpp", "c++" -> "#include <bits/stdc++.h>\nusing namespace std;\nint main(){\n    // TODO\n    return 0;\n}\n";
            default -> "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // TODO\n    }\n}\n";
        };
    }

    /**
     * AI analysis tool used by DsaAssessmentAgent after real test execution.
     * OpenRouter is used when configured; otherwise a deterministic heuristic fallback
     * keeps local development functional.
     */
    public Analysis analyzeCode(String taskTitle, String language, String code, int passed, int total) {
        if (isOpenRouterConfigured()) {
            try {
                Analysis ai = analyzeWithOpenRouter(taskTitle, language, code, passed, total);
                if (ai != null) return ai;
            } catch (Exception ignored) {
                // The agent deliberately falls back instead of losing a submission when the AI provider is unavailable.
            }
        }
        return analyzeHeuristic(code, passed, total);
    }

    private Analysis analyzeWithOpenRouter(String taskTitle, String language, String code, int passed, int total) throws Exception {
        double correctness = total == 0 ? 0 : round(2.5 * passed / total);
        String prompt = """
            You are the code-analysis tool inside a DSA assessment agent.
            The code has already been executed by a sandbox. Do NOT invent test results.
            Analyze only algorithmic efficiency, likely time/space complexity, code quality, and edge-case robustness.
            Return ONLY JSON with this exact shape:
            {
              \"timeComplexity\":\"O(...)\",
              \"spaceComplexity\":\"O(...)\",
              \"efficiencyScore\":0.0,
              \"qualityScore\":0.0,
              \"feedback\":\"concise feedback\"
            }
            efficiencyScore must be between 0 and 1.25.
            qualityScore must be between 0 and 1.25.
            Correctness is controlled by the backend and is NOT part of these two scores.
            Problem: %s
            Language: %s
            Tests passed: %d/%d
            Source code:\n%s
            """.formatted(taskTitle == null ? "Coding problem" : taskTitle,
                language == null ? "unknown" : language, passed, total, code == null ? "" : code);

        String text = callOpenRouterForText(prompt);
        text = text.replace("```json", "").replace("```", "").trim();
        JsonNode json = mapper.readTree(text);

        double efficiency = clamp(json.path("efficiencyScore").asDouble(0.75), 0, 1.25);
        double quality = clamp(json.path("qualityScore").asDouble(0.75), 0, 1.25);
        double score = round(Math.min(5.0, correctness + efficiency + quality));
        String feedback = passed + "/" + total + " tests passed. " + json.path("feedback").asText("AI analysis completed.");
        return new Analysis(score, correctness, round(efficiency), round(quality), feedback,
                json.path("timeComplexity").asText("Not determined"),
                json.path("spaceComplexity").asText("Not determined"), "OPENROUTER_AGENT");
    }

    private boolean isOpenRouterConfigured() {
        return openRouterApiKey != null && !openRouterApiKey.isBlank()
                && openRouterPrimaryModel != null && !openRouterPrimaryModel.isBlank();
    }

    private String callOpenRouterForText(String prompt) throws Exception {
        Exception primaryFailure = null;
        try {
            return callOpenRouterModel(openRouterPrimaryModel, prompt);
        } catch (Exception ex) {
            primaryFailure = ex;
        }

        if (openRouterFallbackModel != null && !openRouterFallbackModel.isBlank()
                && !openRouterFallbackModel.equals(openRouterPrimaryModel)) {
            return callOpenRouterModel(openRouterFallbackModel, prompt);
        }
        throw primaryFailure;
    }

    private String callOpenRouterModel(String model, String prompt) throws Exception {
        String url = "https://openrouter.ai/api/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openRouterApiKey);
        if (openRouterSiteUrl != null && !openRouterSiteUrl.isBlank()) {
            headers.set("HTTP-Referer", openRouterSiteUrl);
        }
        if (openRouterAppName != null && !openRouterAppName.isBlank()) {
            headers.set("X-Title", openRouterAppName);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.2);

        ResponseEntity<String> response = rest.postForEntity(
                url, new HttpEntity<>(body, headers), String.class);
        JsonNode root = mapper.readTree(response.getBody());
        String text = root.path("choices").path(0).path("message").path("content").asText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("OpenRouter returned an empty response for model " + model);
        }
        return text;
    }

    private Analysis analyzeHeuristic(String code,int passed,int total){
        String c=code==null?"":code;
        double correctness= total==0?0:2.5*passed/total;
        boolean nested=c.matches("(?s).*for\\s*\\([^)]*\\).*for\\s*\\([^)]*\\).*") || c.matches("(?s).*while\\s*\\([^)]*\\).*while\\s*\\([^)]*\\).*");
        String time=nested?"O(n²) (heuristic)":(c.contains("HashMap")||c.contains("Map<")||c.contains("Set<")||c.contains("dict")||c.contains("set("))?"O(n) expected (heuristic)":"O(n)–O(n log n) estimated";
        String space=(c.contains("HashMap")||c.contains("Map<")||c.contains("Set<")||c.contains("dict")||c.contains("new int[")||c.contains("new Array"))?"O(n) estimated":"O(1)–O(n) estimated";
        double efficiency=nested?0.55:1.25;
        double quality=0.75;
        if(c.length()<40) quality=0.35;
        if(c.contains("//")||c.contains("/*")||c.contains("# ")) quality=Math.min(1.25,quality+.2);
        if(c.length()>80) quality=Math.min(1.25,quality+.15);
        double totalScore=Math.min(5.0,correctness+efficiency+quality);
        String feedback=passed+"/"+total+" tests passed. "+(nested?"The solution appears to use nested iteration; consider a more efficient approach when possible. ":"The code does not show an obvious quadratic nested-loop pattern. ")+"Complexity values are fallback estimates because the external AI analyzer is not configured.";
        return new Analysis(round(totalScore),round(correctness),round(efficiency),round(quality),feedback,time,space,"HEURISTIC_FALLBACK");
    }

    private double clamp(double v,double min,double max){ return Math.max(min,Math.min(max,v)); }
    private double round(double v){return Math.round(v*10.0)/10.0;}
    public record Analysis(double score,double correctness,double efficiency,double quality,String feedback,String time,String space,String mode){}
}
