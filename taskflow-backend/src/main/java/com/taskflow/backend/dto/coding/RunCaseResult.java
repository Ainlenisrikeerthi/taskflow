package com.taskflow.backend.dto.coding;

public class RunCaseResult {
    public int caseNumber;
    public String input;
    public String expectedOutput;
    public String actualOutput;
    public boolean passed;
    public String error;

    public RunCaseResult() {}
    public RunCaseResult(int caseNumber, String input, String expectedOutput, String actualOutput, boolean passed, String error) {
        this.caseNumber = caseNumber;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.actualOutput = actualOutput;
        this.passed = passed;
        this.error = error;
    }
}
