package com.taskflow.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class TaskRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    @NotBlank(message = "Task description is required")
    private String description;

    private String instructions;

    @NotNull(message = "Deadline is required")
    private LocalDate deadline;

    private String proofRequirement;

    private String status; // "DRAFT" or "PUBLISHED"

    public TaskRequest() {}

    public TaskRequest(String title, String description, String instructions, LocalDate deadline, String proofRequirement, String status) {
        this.title = title;
        this.description = description;
        this.instructions = instructions;
        this.deadline = deadline;
        this.proofRequirement = proofRequirement;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getProofRequirement() {
        return proofRequirement;
    }

    public void setProofRequirement(String proofRequirement) {
        this.proofRequirement = proofRequirement;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
