import os
import sys
import json
import uuid
import shutil
import tempfile
import logging
import subprocess
import html
import queue
from datetime import datetime, timezone
import requests
from django.conf import settings
from django.core.mail import EmailMultiAlternatives
from .models import User, Task, Assignment, Notification, CodingTestCase, CodeSubmission
from .serializers import serialize_notification

logger = logging.getLogger(__name__)

# -----------------------------------------------------------------------------
# SSE Pub/Sub for In-App Notifications
# -----------------------------------------------------------------------------
_sse_subscribers = {}  # email -> list of queue.Queue()


def sse_subscribe(email):
    q = queue.Queue(maxsize=50)
    if email not in _sse_subscribers:
        _sse_subscribers[email] = []
    _sse_subscribers[email].append(q)
    return q


def sse_unsubscribe(email, q):
    if email in _sse_subscribers:
        try:
            _sse_subscribers[email].remove(q)
            if not _sse_subscribers[email]:
                del _sse_subscribers[email]
        except (ValueError, KeyError):
            pass


def sse_push(email, event_type, data):
    if email in _sse_subscribers:
        for q in list(_sse_subscribers[email]):
            try:
                q.put_nowait({"event": event_type, "data": data})
            except queue.Full:
                pass


# -----------------------------------------------------------------------------
# Notification Service
# -----------------------------------------------------------------------------
class NotificationService:
    @staticmethod
    def create(user, n_type, title, message, task=None):
        n = Notification.objects.create(
            user=user,
            type=n_type,
            title=title,
            message=message,
            task=task,
            is_read=False
        )
        data = serialize_notification(n)
        sse_push(user.email, "notification", data)
        return n

    @staticmethod
    def create_for_users(users, n_type, title, message, task=None):
        for user in users:
            NotificationService.create(user, n_type, title, message, task)


# -----------------------------------------------------------------------------
# Email Service
# -----------------------------------------------------------------------------
class EmailService:
    @staticmethod
    def send_password_reset_email(to_email, user_name, reset_link):
        subject = "TaskFlow — Reset Your Password"
        from_email = settings.DEFAULT_FROM_EMAIL

        html_body = f"""<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
<h2 style="color:#4f46e5;">TaskFlow</h2>
<p>Hello <strong>{html.escape(user_name or 'User')}</strong>,</p>
<p>We received a request to reset your password. Click the button below to set a new password:</p>
<p style="text-align:center;margin:32px 0;">
<a href="{reset_link}" style="background:#4f46e5;color:#fff;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold;display:inline-block;">Reset Password</a>
</p>
<p style="font-size:13px;color:#666;">This link will expire in <strong>1 hour</strong>.</p>
<p style="font-size:13px;color:#666;">If you did not request a password reset, please ignore this email. Your password will remain unchanged.</p>
<hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
<p style="font-size:12px;color:#999;">TaskFlow Team</p>
</div>"""
        text_body = f"Hello {user_name},\n\nReset your password here: {reset_link}\n\nTaskFlow Team"

        try:
            msg = EmailMultiAlternatives(subject, text_body, from_email, [to_email])
            msg.attach_alternative(html_body, "text/html")
            msg.send()
            logger.info("[EMAIL] Password reset email sent to %s", to_email)
        except Exception as e:
            logger.error("[EMAIL] Failed to send password reset email to %s: %s", to_email, str(e))
            raise e

    @staticmethod
    def send_assignment_removal_email(to_email, user_name, task_title, current_status, removal_reason):
        subject = "TaskFlow — Assignment Removed"
        from_email = settings.DEFAULT_FROM_EMAIL

        reason_section = (
            f"<p><strong>Reason:</strong> {html.escape(removal_reason.strip())}</p>"
            if removal_reason and removal_reason.strip()
            else "<p><strong>Reason:</strong> No specific reason provided.</p>"
        )

        html_body = f"""<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
<h2 style="color:#4f46e5;">TaskFlow</h2>
<p>Hello <strong>{html.escape(user_name or 'User')}</strong>,</p>
<p>Your assignment for <strong>"{html.escape(task_title)}"</strong> has been removed by an administrator.</p>
{reason_section}
<p>You can view the updated status in your TaskFlow history.</p>
<hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
<p style="font-size:12px;color:#999;">TaskFlow Team</p>
</div>"""
        text_body = f"Hello {user_name},\n\nYour assignment for '{task_title}' was removed.\nReason: {removal_reason or 'No reason provided'}\n\nTaskFlow Team"

        try:
            msg = EmailMultiAlternatives(subject, text_body, from_email, [to_email])
            msg.attach_alternative(html_body, "text/html")
            msg.send()
            logger.info("[EMAIL] Assignment removal email sent to %s", to_email)
        except Exception as e:
            logger.error("[EMAIL] Assignment removal email to %s failed: %s", to_email, str(e))

    @staticmethod
    def send_deadline_reminder_email(to_email, user_name, task_title, deadline, overdue):
        subject = "TaskFlow — Task Overdue" if overdue else "TaskFlow — Task Due Tomorrow"
        status_line = "is overdue" if overdue else "is due tomorrow"
        from_email = settings.DEFAULT_FROM_EMAIL

        html_body = f"""<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
<h2 style="color:#6b3f2a;">TaskFlow</h2>
<p>Hello <strong>{html.escape(user_name or 'User')}</strong>,</p>
<p>Your task <strong>"{html.escape(task_title)}"</strong> {status_line}.</p>
<p><strong>Deadline:</strong> {deadline}</p>
<p>Please open TaskFlow to review or update your progress.</p>
<hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
<p style="font-size:12px;color:#999;">TaskFlow Team</p>
</div>"""
        try:
            msg = EmailMultiAlternatives(subject, f"Task {task_title} {status_line}", from_email, [to_email])
            msg.attach_alternative(html_body, "text/html")
            msg.send()
        except Exception as e:
            logger.warn("[EMAIL] Deadline email failed for %s: %s", to_email, str(e))


# -----------------------------------------------------------------------------
# Local Code Execution Service
# -----------------------------------------------------------------------------
class CodeExecutionService:
    @staticmethod
    def normalize_language(language):
        val = (language or "java").strip().lower()
        if val in ("java",):
            return "java"
        if val in ("python", "py"):
            return "python"
        if val in ("javascript", "js", "node", "nodejs"):
            return "javascript"
        if val in ("c",):
            return "c"
        if val in ("cpp", "c++"):
            return "cpp"
        return None

    @staticmethod
    def run(language, code, stdin=""):
        if not code or not code.strip():
            return {"stdout": "", "stderr": "Source code is empty.", "exitCode": -1}

        lang = CodeExecutionService.normalize_language(language)
        if not lang:
            return {"stdout": "", "stderr": "Unsupported language. Choose Java, Python, JavaScript, C, or C++.", "exitCode": -1}

        if lang == "java" and ("public class Main" not in code or "static void main" not in code):
            return {"stdout": "", "stderr": "Java solution must contain 'public class Main' with a main method.", "exitCode": -1}

        temp_dir = tempfile.mkdtemp(prefix="taskflow-code-")
        try:
            file_names = {
                "java": "Main.java",
                "python": "Main.py",
                "javascript": "Main.js",
                "c": "Main.c",
                "cpp": "Main.cpp"
            }
            source_file = os.path.join(temp_dir, file_names[lang])
            with open(source_file, "w", encoding="utf-8") as f:
                f.write(code)

            # Compilation if needed
            compile_timeout = settings.LOCAL_COMPILE_TIMEOUT_SECONDS
            run_timeout = settings.LOCAL_RUN_TIMEOUT_SECONDS
            max_chars = settings.LOCAL_MAX_OUTPUT_CHARS

            if lang == "java":
                res = subprocess.run(
                    [settings.LOCAL_JAVAC_COMMAND, "-encoding", "UTF-8", "Main.java"],
                    cwd=temp_dir,
                    capture_output=True,
                    text=True,
                    timeout=compile_timeout
                )
                if res.returncode != 0:
                    err = res.stderr or res.stdout or "Compilation error"
                    return {"stdout": "", "stderr": f"Compilation error: {err[:max_chars]}", "exitCode": -1}
                run_cmd = [settings.LOCAL_JAVA_COMMAND, "-Xms16m", "-Xmx128m", "-cp", temp_dir, "Main"]

            elif lang == "python":
                run_cmd = [settings.LOCAL_PYTHON_COMMAND, "Main.py"]

            elif lang == "javascript":
                run_cmd = ["node", "Main.js"]

            elif lang == "c":
                exe = os.path.join(temp_dir, "Main.exe" if os.name == 'nt' else "Main")
                res = subprocess.run(
                    ["gcc", "Main.c", "-o", exe],
                    cwd=temp_dir,
                    capture_output=True,
                    text=True,
                    timeout=compile_timeout
                )
                if res.returncode != 0:
                    err = res.stderr or res.stdout or "Compilation error"
                    return {"stdout": "", "stderr": f"Compilation error: {err[:max_chars]}", "exitCode": -1}
                run_cmd = [exe]

            elif lang == "cpp":
                exe = os.path.join(temp_dir, "Main.exe" if os.name == 'nt' else "Main")
                res = subprocess.run(
                    ["g++", "-std=c++17", "Main.cpp", "-o", exe],
                    cwd=temp_dir,
                    capture_output=True,
                    text=True,
                    timeout=compile_timeout
                )
                if res.returncode != 0:
                    err = res.stderr or res.stdout or "Compilation error"
                    return {"stdout": "", "stderr": f"Compilation error: {err[:max_chars]}", "exitCode": -1}
                run_cmd = [exe]

            # Run process
            run_res = subprocess.run(
                run_cmd,
                cwd=temp_dir,
                input=stdin or "",
                capture_output=True,
                text=True,
                timeout=run_timeout
            )
            stdout = (run_res.stdout or "")[:max_chars]
            stderr = (run_res.stderr or "")[:max_chars]
            return {"stdout": stdout, "stderr": stderr, "exitCode": run_res.returncode}

        except subprocess.TimeoutExpired:
            return {"stdout": "", "stderr": f"Execution timed out after {settings.LOCAL_RUN_TIMEOUT_SECONDS} seconds.", "exitCode": -1}
        except Exception as e:
            return {"stdout": "", "stderr": f"Local runner error: {str(e)}", "exitCode": -1}
        finally:
            shutil.rmtree(temp_dir, ignore_errors=True)


# -----------------------------------------------------------------------------
# DSA Assessment Agent & AI Tool
# -----------------------------------------------------------------------------
class CodingAgentService:
    @staticmethod
    def normalize_str(s):
        if not s:
            return ""
        return s.replace("\r\n", "\n").replace("\r", "\n").strip()

    @staticmethod
    def generate(title, difficulty, language):
        title = (title or "Coding Challenge").strip()
        difficulty = (difficulty or "MEDIUM").upper()
        language = language or "java"

        api_key = settings.OPENROUTER_API_KEY
        primary_model = settings.OPENROUTER_PRIMARY_MODEL

        if api_key and primary_model:
            try:
                prompt = f"""You are a DSA assessment author. Create a complete coding problem for an admin to review before publishing.
Return ONLY valid JSON with this exact shape:
{{
  "description": "clear problem statement with input/output format and examples",
  "instructions": "constraints and important notes",
  "starterCode": "starter code for the requested language",
  "testCases": [
    {{"input":"...","expectedOutput":"...","hidden":false}},
    {{"input":"...","expectedOutput":"...","hidden":true}}
  ]
}}
Generate at least 5 test cases: 2 visible and 3 hidden, including normal, boundary, duplicate/edge, and larger cases where relevant.
Do not include markdown fences.
Title: {title}
Difficulty: {difficulty}
Language: {language}
"""
                headers = {
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": settings.OPENROUTER_SITE_URL,
                    "X-Title": settings.OPENROUTER_APP_NAME
                }
                body = {
                    "model": primary_model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.2
                }
                resp = requests.post("https://openrouter.ai/api/v1/chat/completions", headers=headers, json=body, timeout=20)
                if resp.status_code == 200:
                    content = resp.json()["choices"][0]["message"]["content"]
                    cleaned = content.replace("```json", "").replace("```", "").strip()
                    parsed = json.loads(cleaned)
                    return {
                        "description": parsed.get("description", ""),
                        "instructions": parsed.get("instructions", ""),
                        "starterCode": parsed.get("starterCode", ""),
                        "testCases": parsed.get("testCases", []),
                        "agentTrace": None
                    }
            except Exception as e:
                logger.warn("OpenRouter generation fallback triggered: %s", str(e))

        # Fallback Deterministic Generator
        return CodingAgentService.fallback_generate(title, difficulty, language)

    @staticmethod
    def fallback_generate(title, difficulty, language):
        key = title.lower()
        starter = CodingAgentService.get_starter(language)

        if "two sum" in key:
            return {
                "description": "Given an array of integers and a target, print the zero-based indices of two distinct elements whose sum equals the target.\n\nInput format:\nFirst line: n\nSecond line: n space-separated integers\nThird line: target\n\nOutput format:\nPrint the two indices in increasing order separated by one space.",
                "instructions": f"Difficulty: {difficulty}. Exactly one valid pair exists. Aim for O(n) time using a hash-based approach. Admin should review all generated cases before publishing.",
                "starterCode": starter,
                "testCases": [
                    {"input": "4\n2 7 11 15\n9", "expectedOutput": "0 1", "hidden": False},
                    {"input": "3\n3 2 4\n6", "expectedOutput": "1 2", "hidden": False},
                    {"input": "2\n3 3\n6", "expectedOutput": "0 1", "hidden": True},
                    {"input": "5\n-3 4 3 90 1\n0", "expectedOutput": "0 2", "hidden": True},
                    {"input": "6\n1 5 8 2 9 4\n13", "expectedOutput": "1 2", "hidden": True},
                ],
                "agentTrace": None
            }
        elif "palindrome" in key:
            return {
                "description": "Read a single string and print true if it reads the same forward and backward; otherwise print false. Comparison is case-sensitive unless the admin changes this requirement.",
                "instructions": f"Difficulty: {difficulty}. Handle empty/single-character style edge cases according to the input constraints. Prefer O(n) time and O(1) extra space when possible.",
                "starterCode": starter,
                "testCases": [
                    {"input": "racecar", "expectedOutput": "true", "hidden": False},
                    {"input": "hello", "expectedOutput": "false", "hidden": False},
                    {"input": "a", "expectedOutput": "true", "hidden": True},
                    {"input": "abba", "expectedOutput": "true", "hidden": True},
                    {"input": "abca", "expectedOutput": "false", "hidden": True},
                ],
                "agentTrace": None
            }
        elif "factorial" in key:
            return {
                "description": "Read a non-negative integer n and print n factorial.",
                "instructions": f"Difficulty: {difficulty}. 0! = 1. Admin should set a numeric range appropriate for the chosen language.",
                "starterCode": starter,
                "testCases": [
                    {"input": "5", "expectedOutput": "120", "hidden": False},
                    {"input": "0", "expectedOutput": "1", "hidden": False},
                    {"input": "1", "expectedOutput": "1", "hidden": True},
                    {"input": "6", "expectedOutput": "720", "hidden": True},
                    {"input": "10", "expectedOutput": "3628800", "hidden": True},
                ],
                "agentTrace": None
            }
        elif "fibonacci" in key:
            return {
                "description": "Read n and print the nth Fibonacci number using F(0)=0 and F(1)=1.",
                "instructions": f"Difficulty: {difficulty}. Avoid exponential recursion for larger n. Prefer O(n) time and O(1) auxiliary space.",
                "starterCode": starter,
                "testCases": [
                    {"input": "7", "expectedOutput": "13", "hidden": False},
                    {"input": "0", "expectedOutput": "0", "hidden": False},
                    {"input": "1", "expectedOutput": "1", "hidden": True},
                    {"input": "10", "expectedOutput": "55", "hidden": True},
                    {"input": "20", "expectedOutput": "6765", "hidden": True},
                ],
                "agentTrace": None
            }
        else:
            return {
                "description": f"Solve “{title}”. Read the input from standard input and print only the required output. This fallback draft was generated without an external AI model, so the admin must edit the exact input/output specification before publishing.",
                "instructions": f"Difficulty: {difficulty}. Configure OPENROUTER_API_KEY to enable full AI-generated arbitrary DSA descriptions and test cases. The final score considers correctness, efficiency and code quality.",
                "starterCode": starter,
                "testCases": [
                    {"input": "4\n1 2 3 4", "expectedOutput": "10", "hidden": False},
                    {"input": "2\n5 5", "expectedOutput": "10", "hidden": False},
                    {"input": "1\n0", "expectedOutput": "0", "hidden": True},
                    {"input": "3\n-1 -2 -3", "expectedOutput": "-6", "hidden": True},
                    {"input": "5\n10 20 30 40 50", "expectedOutput": "150", "hidden": True},
                ],
                "agentTrace": None
            }

    @staticmethod
    def get_starter(lang):
        l = (lang or "java").lower()
        if l in ("python", "py"):
            return "# Read from stdin and print the answer\n"
        if l in ("javascript", "js"):
            return "// Read from stdin and print the answer\n"
        if l in ("c", "cpp"):
            return "#include <bits/stdc++.h>\nusing namespace std;\nint main(){\n    // TODO\n    return 0;\n}\n"
        return "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // TODO\n    }\n}\n"

    @staticmethod
    def evaluate(task_title, language, code, test_cases):
        passed = 0
        total = len(test_cases)
        errors = []

        for tc in test_cases:
            res = CodeExecutionService.run(language, code, tc.input)
            is_passed = (res["exitCode"] == 0 and 
                         CodingAgentService.normalize_str(res["stdout"]) == CodingAgentService.normalize_str(tc.expected_output))
            if is_passed:
                passed += 1
            elif res["stderr"] and res["stderr"].strip():
                errors.append(res["stderr"].strip())

        # AI or Heuristic Analysis
        analysis = CodingAgentService.analyze_code(task_title, language, code, passed, total)

        feedback = analysis.get("feedback", "")
        if errors:
            feedback += f" Execution note: {' | '.join(errors[:2])}"

        return {
            "passed": passed,
            "total": total,
            "score": analysis["score"],
            "correctness": analysis["correctnessScore"],
            "efficiency": analysis["efficiencyScore"],
            "quality": analysis["qualityScore"],
            "timeComplexity": analysis["timeComplexity"],
            "spaceComplexity": analysis["spaceComplexity"],
            "feedback": feedback
        }

    @staticmethod
    def analyze_code(task_title, language, code, passed, total):
        correctness = round(2.5 * passed / total, 1) if total > 0 else 0.0

        api_key = settings.OPENROUTER_API_KEY
        primary_model = settings.OPENROUTER_PRIMARY_MODEL

        if api_key and primary_model:
            try:
                prompt = f"""You are the code-analysis tool inside a DSA assessment agent.
The code has already been executed by a sandbox. Do NOT invent test results.
Analyze only algorithmic efficiency, likely time/space complexity, code quality, and edge-case robustness.
Return ONLY JSON with this exact shape:
{{
  "timeComplexity":"O(...)",
  "spaceComplexity":"O(...)",
  "efficiencyScore":0.0,
  "qualityScore":0.0,
  "feedback":"concise feedback"
}}
efficiencyScore must be between 0 and 1.25.
qualityScore must be between 0 and 1.25.
Problem: {task_title}
Language: {language}
Tests passed: {passed}/{total}
Source code:
{code}
"""
                headers = {
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": settings.OPENROUTER_SITE_URL,
                    "X-Title": settings.OPENROUTER_APP_NAME
                }
                body = {
                    "model": primary_model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.2
                }
                resp = requests.post("https://openrouter.ai/api/v1/chat/completions", headers=headers, json=body, timeout=20)
                if resp.status_code == 200:
                    content = resp.json()["choices"][0]["message"]["content"]
                    cleaned = content.replace("```json", "").replace("```", "").strip()
                    parsed = json.loads(cleaned)
                    eff = min(1.25, max(0.0, float(parsed.get("efficiencyScore", 0.75))))
                    qual = min(1.25, max(0.0, float(parsed.get("qualityScore", 0.75))))
                    total_score = round(min(5.0, correctness + eff + qual), 1)
                    return {
                        "score": total_score,
                        "correctnessScore": correctness,
                        "efficiencyScore": round(eff, 1),
                        "qualityScore": round(qual, 1),
                        "timeComplexity": parsed.get("timeComplexity", "O(n)"),
                        "spaceComplexity": parsed.get("spaceComplexity", "O(1)"),
                        "feedback": f"{passed}/{total} tests passed. {parsed.get('feedback', '')}"
                    }
            except Exception as e:
                logger.warn("OpenRouter code analysis fallback: %s", str(e))

        # Heuristic Analysis
        c = code or ""
        nested = ("for " in c and c.count("for ") > 1) or ("while " in c and c.count("while ") > 1)
        time_c = "O(n²) (heuristic)" if nested else "O(n) expected (heuristic)"
        space_c = "O(n) estimated" if ("Map" in c or "dict" in c or "set" in c or "new int[" in c) else "O(1) estimated"
        eff = 0.55 if nested else 1.25
        qual = 0.75
        if len(c) < 40:
            qual = 0.35
        if "//" in c or "/*" in c or "#" in c:
            qual = min(1.25, qual + 0.2)
        if len(c) > 80:
            qual = min(1.25, qual + 0.15)
        total_score = round(min(5.0, correctness + eff + qual), 1)
        feedback = f"{passed}/{total} tests passed. " + (
            "The solution appears to use nested iteration; consider a more efficient approach when possible. "
            if nested else "The code does not show an obvious quadratic nested-loop pattern. "
        )
        return {
            "score": total_score,
            "correctnessScore": correctness,
            "efficiencyScore": round(eff, 1),
            "qualityScore": round(qual, 1),
            "timeComplexity": time_c,
            "spaceComplexity": space_c,
            "feedback": feedback
        }
