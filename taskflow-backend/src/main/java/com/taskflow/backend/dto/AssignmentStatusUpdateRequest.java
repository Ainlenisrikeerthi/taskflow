package com.taskflow.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignmentStatusUpdateRequest {

    private Long id; // Optional when passed in URL path

    @NotBlank(message = "Status is required")
    private String status; // "ASSIGNED_NOT_STARTED", "STARTED_NOT_COMPLETED", "COMPLETED"

    private String proofUrl;

    public AssignmentStatusUpdateRequest() {}

    public AssignmentStatusUpdateRequest(Long id, String status, String proofUrl) {
        this.id = id;
        this.status = status;
        this.proofUrl = proofUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }
}
