package com.taskflow.backend.dto;

public class AssignmentRemoveRequest {

    private String reason; // Optional reason for removal

    public AssignmentRemoveRequest() {}

    public AssignmentRemoveRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
