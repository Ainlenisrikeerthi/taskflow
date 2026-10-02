package com.taskflow.backend.service;

import com.taskflow.backend.dto.coding.*;
import com.taskflow.backend.exception.BadRequestException;
import com.taskflow.backend.model.CodingTestCase;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DSA Assessment Agent for TaskFlow.
 *
 * Responsibilities:
 *
 * Admin side:
 * 1. Generate coding question draft
 * 2. Generate visible and hidden test cases
 * 3. Validate generated draft
 * 4. Return draft for Admin review
 *
 * User side:
 * 1. Compile and execute submitted Java code
 * 2. Run visible / hidden test cases
 * 3. Compare actual and expected outputs
 * 4. Analyze complexity and code quality using AI
 * 5. Calculate score out of 5
 *
 * Internal agent implementation details are not exposed
 * to normal users in the API response.
 */
@Service
public class DsaAssessmentAgent {

    private final CodingAgentService aiTool;
    private final CodeExecutionService executionTool;

    public DsaAssessmentAgent(
            CodingAgentService aiTool,
            CodeExecutionService executionTool) {
        this.aiTool = aiTool;
        this.executionTool = executionTool;
    }

    /**
     * ============================================================
     * ADMIN SIDE
     * ============================================================
     *
     * Auto-generate a coding task draft.
     *
     * The AI generates:
     * - problem description
     * - instructions
     * - visible test cases
     * - hidden test cases
     *
     * The draft is validated before being returned.
     *
     * Admin must still review/edit the generated content before
     * saving or publishing.
     */
    public GeneratedCodingTaskResponse generateDraft(
            GenerateCodingTaskRequest request) {

        if (request == null) {
            throw new BadRequestException(
                    "Coding task generation request is required");
        }

        GeneratedCodingTaskResponse draft = aiTool.generate(request);

        validateGeneratedDraft(draft);

        /*
         * Do NOT expose internal agent tool names in UI.
         *
         * Previously:
         *
         * draft.agentTrace =
         * "Agent tools: generateQuestion -> generateTestCases -> validateDraft...";
         *
         * That implementation detail is intentionally removed.
         */

        draft.agentTrace = null;

        return draft;
    }

    /**
     * ============================================================
     * USER SIDE - RUN CODE
     * ============================================================
     *
     * Runs only the visible test cases.
     *
     * No final score is produced here.
     *
     * This allows the user to test their solution before final
     * submission.
     */
    public RunCodeResponse runVisible(
            String language,
            String code,
            List<CodingTestCase> cases) {

        validateCode(code);

        if (cases == null || cases.isEmpty()) {
            throw new BadRequestException(
                    "No visible test cases configured by admin");
        }

        RunCodeResponse response = new RunCodeResponse();

        response.total = cases.size();

        int caseNumber = 1;

        for (CodingTestCase testCase : cases) {

            CodeExecutionService.Result result = executionTool.run(
                    language,
                    code,
                    testCase.getInput());

            boolean passed = result.exitCode() == 0
                    &&
                    normalize(result.stdout())
                            .equals(
                                    normalize(
                                            testCase.getExpectedOutput()));

            if (passed) {
                response.passed++;
            }

            response.cases.add(
                    new RunCaseResult(
                            caseNumber++,
                            testCase.getInput(),
                            testCase.getExpectedOutput(),
                            result.stdout(),
                            passed,
                            result.stderr()));
        }

        return response;
    }

    /**
     * ============================================================
     * USER SIDE - SUBMIT & EVALUATE
     * ============================================================
     *
     * Full agent evaluation workflow:
     *
     * 1. Execute submitted code
     * 2. Run visible + hidden test cases
     * 3. Compare actual output with expected output
     * 4. Send code + correctness results to AI analyzer
     * 5. Analyze:
     * - correctness
     * - time complexity
     * - space complexity
     * - efficiency
     * - code quality
     * 6. Calculate final score out of 5
     *
     * Hidden test case values are never returned to the frontend.
     */
    public Evaluation evaluate(
            String taskTitle,
            String language,
            String code,
            List<CodingTestCase> cases) {

        validateCode(code);

        if (cases == null || cases.isEmpty()) {
            throw new BadRequestException(
                    "No test cases configured by admin");
        }

        int passed = 0;

        StringBuilder runnerErrors = new StringBuilder();

        for (CodingTestCase testCase : cases) {

            CodeExecutionService.Result result = executionTool.run(
                    language,
                    code,
                    testCase.getInput());

            boolean passedCase = result.exitCode() == 0
                    &&
                    normalize(result.stdout())
                            .equals(
                                    normalize(
                                            testCase.getExpectedOutput()));

            if (passedCase) {

                passed++;

            } else {

                if (result.stderr() != null
                        &&
                        !result.stderr().isBlank()) {

                    if (runnerErrors.length() > 0) {
                        runnerErrors.append(" | ");
                    }

                    runnerErrors.append(
                            result.stderr().trim());
                }
            }
        }

        /*
         * ========================================================
         * AI ANALYSIS
         * ========================================================
         *
         * OpenRouter / configured AI model analyzes:
         *
         * - algorithmic approach
         * - efficiency
         * - time complexity
         * - space complexity
         * - code quality
         *
         * Test correctness has already been determined by
         * real code execution.
         */
        CodingAgentService.Analysis analysis = aiTool.analyzeCode(
                taskTitle,
                language,
                code,
                passed,
                cases.size());

        /*
         * ========================================================
         * FEEDBACK
         * ========================================================
         */

        String feedback = analysis.feedback() == null
                ? ""
                : analysis.feedback();

        /*
         * Only append a clean execution error if necessary.
         *
         * Internal agent implementation details are NOT returned.
         */
        if (runnerErrors.length() > 0) {

            if (!feedback.isBlank()) {
                feedback += " ";
            }

            feedback += "Execution note: "
                    + runnerErrors;
        }

        /*
         * ========================================================
         * USER-FACING RESULT
         * ========================================================
         *
         * User sees:
         *
         * Score
         * Passed tests
         * Correctness
         * Efficiency
         * Quality
         * Time complexity
         * Space complexity
         * Feedback
         *
         * User does NOT see:
         *
         * compileJava
         * executeTests
         * compareOutputs
         * OpenRouterAnalyzeComplexity
         * calculateScore
         * persistResult
         */

        return new Evaluation(
                passed,
                cases.size(),

                analysis.score(),

                analysis.correctness(),

                analysis.efficiency(),

                analysis.quality(),

                feedback,

                analysis.time(),

                analysis.space(),

                "AI-assisted assessment");
    }

    /**
     * ============================================================
     * GENERATED DRAFT VALIDATION
     * ============================================================
     *
     * Prevents invalid AI-generated tasks from being returned
     * to Admin.
     */
    private void validateGeneratedDraft(
            GeneratedCodingTaskResponse draft) {

        if (draft == null) {
            throw new BadRequestException(
                    "Agent could not generate a coding task");
        }

        if (draft.description == null
                ||
                draft.description.isBlank()) {
            throw new BadRequestException(
                    "Agent could not generate a valid problem description");
        }

        if (draft.testCases == null
                ||
                draft.testCases.size() < 2) {
            throw new BadRequestException(
                    "Agent must generate at least two test cases");
        }

        boolean hasVisible = false;
        boolean hasHidden = false;

        for (TestCaseDto testCase : draft.testCases) {

            if (testCase == null) {
                throw new BadRequestException(
                        "Agent generated an invalid test case");
            }

            if (testCase.input == null
                    ||
                    testCase.input.isBlank()) {
                throw new BadRequestException(
                        "Agent generated a test case without input");
            }

            if (testCase.expectedOutput == null
                    ||
                    testCase.expectedOutput.isBlank()) {
                throw new BadRequestException(
                        "Agent generated a test case without expected output");
            }

            /*
             * Prevent placeholder AI test cases from being accepted.
             */
            String input = testCase.input
                    .trim()
                    .toLowerCase();

            String output = testCase.expectedOutput
                    .trim()
                    .toLowerCase();

            if (input.contains("sample input")
                    ||
                    output.contains("sample output")
                    ||
                    input.contains("edit_")
                    ||
                    output.contains("edit_")) {
                throw new BadRequestException(
                        "Agent generated placeholder test cases. "
                                +
                                "Please regenerate or edit the test cases.");
            }

            if (testCase.hidden) {

                hasHidden = true;

            } else {

                hasVisible = true;
            }
        }

        if (!hasVisible) {
            throw new BadRequestException(
                    "Agent draft must contain at least one visible test case");
        }

        if (!hasHidden) {
            throw new BadRequestException(
                    "Agent draft must contain at least one hidden test case");
        }
    }

    /**
     * Validate user code.
     */
    private void validateCode(String code) {

        if (code == null
                ||
                code.isBlank()) {
            throw new BadRequestException(
                    "Write some code before running or submitting");
        }
    }

    /**
     * Normalizes output before comparison.
     *
     * Helps avoid false failures caused by:
     *
     * Windows:
     * \r\n
     *
     * Linux:
     * \n
     *
     * and trailing spaces/newlines.
     */
    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    /**
     * ============================================================
     * USER-FACING EVALUATION RESPONSE
     * ============================================================
     *
     * Internal agentTrace has intentionally been removed.
     */
    public record Evaluation(

            int passed,

            int total,

            double score,

            double correctness,

            double efficiency,

            double quality,

            String feedback,

            String timeComplexity,

            String spaceComplexity,

            String evaluationMode

    ) {
    }
}