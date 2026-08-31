package com.taskflow.backend.dto;

import com.taskflow.backend.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private String instructions;
    private LocalDate deadline;
    private String proofRequirement;
    private TaskStatus status;
    private String taskType;
    private String difficulty;
    private String starterCode;
    private Long createdById;
    private String createdByName;
    private long assignedCount;
    private long completedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TaskResponse() {}

    public String getTaskType(){return taskType;} public void setTaskType(String v){taskType=v;}
    public String getDifficulty(){return difficulty;} public void setDifficulty(String v){difficulty=v;}
    public String getStarterCode(){return starterCode;} public void setStarterCode(String v){starterCode=v;}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public long getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(long assignedCount) {
        this.assignedCount = assignedCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
