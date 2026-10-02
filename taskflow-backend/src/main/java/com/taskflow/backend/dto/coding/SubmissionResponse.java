package com.taskflow.backend.dto.coding;

import java.time.LocalDateTime;

public class SubmissionResponse {
    public Long id;
    public Long taskId;
    public String taskTitle;
    public String userName;
    public String language;
    public double score;
    public double correctnessScore;
    public double efficiencyScore;
    public double qualityScore;
    public int passedTests;
    public int totalTests;
    public String feedback;
    public String timeComplexity;
    public String spaceComplexity;
    public LocalDateTime submittedAt;
}
