package com.taskflow.backend.dto;

public class AssignmentUpdateRequest {
    private Long id;
    private String status; // e.g. "ASSIGNED_NOT_STARTED", "STARTED_NOT_COMPLETED", "COMPLETED"
    private String proofUrl;

    public AssignmentUpdateRequest() {}

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
