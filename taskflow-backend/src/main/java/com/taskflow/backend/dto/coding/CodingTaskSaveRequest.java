package com.taskflow.backend.dto.coding;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CodingTaskSaveRequest {
    public String title;
    public String difficulty;
    public LocalDate deadline;
    public String description;
    public String instructions;
    public String starterCode;
    public String status;
    public List<TestCaseDto> testCases = new ArrayList<>();
}
