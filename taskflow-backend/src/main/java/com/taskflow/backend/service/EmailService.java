package com.taskflow.backend.service;

public interface EmailService {

    /**
     * Send a password reset link email.
     */
    void sendPasswordResetEmail(String toEmail, String userName, String resetLink);

    /**
     * Send an assignment removal notification to the affected user.
     */
    void sendAssignmentRemovalEmail(String toEmail, String userName, String taskTitle, String currentStatus, String removalReason);

    /** Send a task deadline reminder email. */
    void sendDeadlineReminderEmail(String toEmail, String userName, String taskTitle, java.time.LocalDate deadline, boolean overdue);
}
