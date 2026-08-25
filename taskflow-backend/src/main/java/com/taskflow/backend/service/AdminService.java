package com.taskflow.backend.service;

import com.taskflow.backend.dto.AssignmentResponse;
import com.taskflow.backend.dto.DashboardStatsResponse;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.model.Assignment;
import com.taskflow.backend.model.AssignmentStatus;
import com.taskflow.backend.repository.AssignmentRepository;
import com.taskflow.backend.repository.AssignmentSpecification;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalTasks = taskRepository.count();
        long totalUsers = userRepository.count();
        long totalAssignments = assignmentRepository.countByIsActiveTrue();
        long completedAssignments = assignmentRepository.countByStatusAndIsActiveTrue(AssignmentStatus.COMPLETED);
        long inProgressAssignments = assignmentRepository.countByStatusAndIsActiveTrue(AssignmentStatus.STARTED_NOT_COMPLETED);
        long notStartedAssignments = assignmentRepository.countByStatusAndIsActiveTrue(AssignmentStatus.ASSIGNED_NOT_STARTED);

        List<AssignmentResponse> recentAssignments = assignmentRepository.findTop10ByOrderByAssignedAtDesc().stream()
                .map(assignmentService::mapToAssignmentResponse)
                .collect(Collectors.toList());

        return new DashboardStatsResponse(
                totalTasks,
                totalUsers,
                totalAssignments,
                completedAssignments,
                inProgressAssignments,
                notStartedAssignments,
                recentAssignments
        );
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(String searchTerm, String statusStr, Long taskId) {
        AssignmentStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                String normalized = statusStr.trim().toUpperCase();
                if (normalized.equals("IN_PROGRESS") || normalized.equals("STARTED")) {
                    status = AssignmentStatus.STARTED_NOT_COMPLETED;
                } else if (normalized.equals("ASSIGNED") || normalized.equals("NOT_STARTED")) {
                    status = AssignmentStatus.ASSIGNED_NOT_STARTED;
                } else {
                    status = AssignmentStatus.valueOf(normalized);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        String search = (searchTerm == null || searchTerm.trim().isEmpty()) ? null : searchTerm.trim();

        return assignmentRepository.findAll(AssignmentSpecification.filterAdminAssignments(search, status, taskId)).stream()
                .map(assignmentService::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByTaskId(Long taskId) {
        return assignmentRepository.findByTaskId(taskId).stream()
                .map(assignmentService::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByUserId(Long userId) {
        return assignmentRepository.findByUserId(userId).stream()
                .map(assignmentService::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssignmentResponse removeAssignment(Long assignmentId, String reason, String adminEmail) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        String originalStatus = assignment.getStatus() != null ? assignment.getStatus().name() : "ASSIGNED";
        String taskTitle = assignment.getTask() != null ? assignment.getTask().getTitle() : "Task";
        String userEmail = assignment.getUser() != null ? assignment.getUser().getEmail() : null;
        String userName = assignment.getUser() != null ? assignment.getUser().getName() : "User";

        // Admin can remove assignment at ANY stage
        assignment.setIsActive(false);
        assignment.setStatus(AssignmentStatus.REMOVED);
        assignment.setRemovedAt(LocalDateTime.now());
        assignment.setRemovedReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : null);

        Assignment savedAssignment = assignmentRepository.save(assignment);
        if (assignment.getUser() != null) {
            notificationService.create(assignment.getUser(), com.taskflow.backend.model.NotificationType.ASSIGNMENT_REMOVED,
                    "Assignment removed", "Your assignment for “" + taskTitle + "” was removed by an administrator.", assignment.getTask());
        }

        // Send email notification to the affected user (non-critical: log failures but do NOT roll back DB)
        if (userEmail != null) {
            try {
                emailService.sendAssignmentRemovalEmail(
                        userEmail,
                        userName,
                        taskTitle,
                        originalStatus,
                        reason
                );
            } catch (Exception emailEx) {
                // Log clearly. The DB change is already committed; we cannot and should not roll back.
                // The admin sees success in the UI; SMTP must be fixed separately.
                org.slf4j.LoggerFactory.getLogger(AdminService.class).error(
                        "[EMAIL] Assignment #{} removed from DB, but removal email to {} FAILED: {}",
                        assignmentId, userEmail, emailEx.getMessage()
                );
                // Do NOT re-throw — rethrowing rolls back the @Transactional block!
            }
        }

        return assignmentService.mapToAssignmentResponse(savedAssignment);
    }
}
