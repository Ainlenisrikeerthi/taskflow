package com.taskflow.backend.service;

import com.taskflow.backend.dto.AssignmentResponse;
import com.taskflow.backend.dto.AssignmentStatusUpdateRequest;
import com.taskflow.backend.exception.BadRequestException;
import com.taskflow.backend.exception.DuplicateResourceException;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.exception.UnauthorizedAccessException;
import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.AssignmentRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMyActiveAssignments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return assignmentRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMyAllAssignments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return assignmentRepository.findByUserId(user.getId()).stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id, String userEmail) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Allow if user is owner or Admin
        if (!assignment.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("You are not authorized to view this assignment.");
        }

        return mapToAssignmentResponse(assignment);
    }

    @Transactional
    public AssignmentResponse assignTask(Long taskId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new BadRequestException("Cannot assign a task that is not published.");
        }

        // Prevent duplicate active assignments
        boolean exists = assignmentRepository.existsByUserIdAndTaskIdAndIsActiveTrue(user.getId(), task.getId());
        if (exists) {
            throw new DuplicateResourceException("You already have an active assignment for this task.");
        }

        Assignment assignment = new Assignment();
        assignment.setTask(task);
        assignment.setUser(user);
        assignment.setStatus(AssignmentStatus.ASSIGNED_NOT_STARTED);
        assignment.setIsActive(true);
        assignment.setAssignedAt(LocalDateTime.now());

        Assignment savedAssignment = assignmentRepository.save(assignment);
        notificationService.create(user, NotificationType.ASSIGNMENT_CREATED, "Task assigned",
                "You assigned yourself to “" + task.getTitle() + "”.", task);
        return mapToAssignmentResponse(savedAssignment);
    }

    @Transactional
    public AssignmentResponse unassignTask(Long assignmentId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (!assignment.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("You can only unassign your own tasks.");
        }

        if (!assignment.getIsActive()) {
            throw new BadRequestException("This assignment is already inactive or removed.");
        }

        assignment.setIsActive(false);
        assignment.setStatus(AssignmentStatus.REMOVED);
        assignment.setRemovedAt(LocalDateTime.now());
        assignment.setRemovedReason("Self-unassigned by user");

        Assignment updatedAssignment = assignmentRepository.save(assignment);
        notificationService.create(assignment.getUser(), NotificationType.ASSIGNMENT_REMOVED, "Task unassigned",
                "Your assignment for “" + assignment.getTask().getTitle() + "” was removed.", assignment.getTask());
        return mapToAssignmentResponse(updatedAssignment);
    }

    @Transactional
    public AssignmentResponse unassignByTaskId(Long taskId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        List<Assignment> activeAssignments = assignmentRepository.findByUserIdAndIsActiveTrue(user.getId());
        Assignment assignment = activeAssignments.stream()
                .filter(a -> a.getTask().getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active assignment found for task id: " + taskId));

        return unassignTask(assignment.getId(), userEmail);
    }

    @Transactional
    public AssignmentResponse updateStatus(Long assignmentId, AssignmentStatusUpdateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (!assignment.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("You can only update your own assignments.");
        }

        if (!assignment.getIsActive()) {
            throw new BadRequestException("Cannot update an inactive or removed assignment.");
        }

        AssignmentStatus targetStatus = parseStatus(request.getStatus());

        if (targetStatus == AssignmentStatus.COMPLETED) {
            String proofUrl = request.getProofUrl();
            if (proofUrl == null || proofUrl.trim().isEmpty()) {
                throw new BadRequestException("Proof URL is required when completing a task.");
            }
            validateUrl(proofUrl.trim());
            assignment.setProofUrl(proofUrl.trim());
            assignment.setSubmittedAt(LocalDateTime.now());
        } else {
            if (request.getProofUrl() != null) {
                assignment.setProofUrl(request.getProofUrl().trim());
            }
        }

        AssignmentStatus previousStatus = assignment.getStatus();
        assignment.setStatus(targetStatus);
        Assignment updatedAssignment = assignmentRepository.save(assignment);
        if (targetStatus == AssignmentStatus.COMPLETED && previousStatus != AssignmentStatus.COMPLETED) {
            notificationService.create(assignment.getUser(), NotificationType.TASK_COMPLETED, "Task completed",
                    "You completed “" + assignment.getTask().getTitle() + "”.", assignment.getTask());
            notificationService.createForUsers(userRepository.findByRole(Role.ADMIN), NotificationType.TASK_COMPLETED,
                    "Task completed", assignment.getUser().getName() + " completed “" + assignment.getTask().getTitle() + "”.", assignment.getTask());
        }
        return mapToAssignmentResponse(updatedAssignment);
    }

    private AssignmentStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new BadRequestException("Status cannot be empty.");
        }
        String normalized = statusStr.trim().toUpperCase();
        if (normalized.equals("IN_PROGRESS") || normalized.equals("STARTED")) {
            return AssignmentStatus.STARTED_NOT_COMPLETED;
        }
        if (normalized.equals("ASSIGNED") || normalized.equals("NOT_STARTED")) {
            return AssignmentStatus.ASSIGNED_NOT_STARTED;
        }
        try {
            return AssignmentStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid assignment status: " + statusStr);
        }
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https"))) {
                throw new BadRequestException("Proof URL must be a valid HTTP or HTTPS URL.");
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid proof URL format: " + url);
        }
    }

    public AssignmentResponse mapToAssignmentResponse(Assignment assignment) {
        if (assignment == null) return null;

        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        if (assignment.getTask() != null) {
            response.setTaskId(assignment.getTask().getId());
            response.setTask(taskService.mapToTaskResponse(assignment.getTask()));
        }
        if (assignment.getUser() != null) {
            response.setUserId(assignment.getUser().getId());
            response.setUser(userService.mapToUserResponse(assignment.getUser()));
        }
        response.setStatus(assignment.getStatus());
        response.setProofUrl(assignment.getProofUrl());
        response.setIsActive(assignment.getIsActive());
        response.setAssignedAt(assignment.getAssignedAt());
        response.setSubmittedAt(assignment.getSubmittedAt());
        response.setRemovedAt(assignment.getRemovedAt());
        response.setRemovedReason(assignment.getRemovedReason());

        return response;
    }
}
