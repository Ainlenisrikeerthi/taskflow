package com.taskflow.backend.service;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.exception.BadRequestException;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.model.AssignmentStatus;
import com.taskflow.backend.model.Task;
import com.taskflow.backend.model.TaskStatus;
import com.taskflow.backend.model.User;
import com.taskflow.backend.repository.AssignmentRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getPublishedTasks() {
        return taskRepository.findByStatus(TaskStatus.PUBLISHED).stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminEmail));

        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription().trim());
        task.setInstructions(request.getInstructions());
        task.setDeadline(request.getDeadline());
        task.setProofRequirement(request.getProofRequirement());
        task.setCreatedBy(admin);

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                task.setStatus(TaskStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                task.setStatus(TaskStatus.DRAFT);
            }
        } else {
            task.setStatus(TaskStatus.DRAFT);
        }

        Task savedTask = taskRepository.save(task);
        if (savedTask.getStatus() == TaskStatus.PUBLISHED) {
            notificationService.createForUsers(userRepository.findByRole(com.taskflow.backend.model.Role.USER),
                    com.taskflow.backend.model.NotificationType.TASK_PUBLISHED,
                    "New task published", "A new task “" + savedTask.getTitle() + "” is now available.", savedTask);
        }
        return mapToTaskResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        TaskStatus previousStatus = task.getStatus();

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            task.setDescription(request.getDescription().trim());
        }
        if (request.getInstructions() != null) {
            task.setInstructions(request.getInstructions());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getProofRequirement() != null) {
            task.setProofRequirement(request.getProofRequirement());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                task.setStatus(TaskStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid task status: " + request.getStatus());
            }
        }

        Task updatedTask = taskRepository.save(task);
        if (updatedTask.getStatus() == TaskStatus.PUBLISHED) {
            if (previousStatus != TaskStatus.PUBLISHED) {
                notificationService.createForUsers(userRepository.findByRole(com.taskflow.backend.model.Role.USER),
                        com.taskflow.backend.model.NotificationType.TASK_PUBLISHED, "Task published",
                        "“" + updatedTask.getTitle() + "” is now available.", updatedTask);
            } else {
                java.util.List<User> recipients = assignmentRepository.findByTaskIdAndIsActiveTrue(id).stream()
                        .map(a -> a.getUser()).distinct().collect(java.util.stream.Collectors.toList());
                notificationService.createForUsers(recipients, com.taskflow.backend.model.NotificationType.TASK_UPDATED,
                        "Task updated", "“" + updatedTask.getTitle() + "” has been updated.", updatedTask);
            }
        }
        return mapToTaskResponse(updatedTask);
    }

    @Transactional
    public TaskResponse publishTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        boolean wasPublished = task.getStatus() == TaskStatus.PUBLISHED;
        task.setStatus(TaskStatus.PUBLISHED);
        Task updatedTask = taskRepository.save(task);
        if (!wasPublished) {
            notificationService.createForUsers(userRepository.findByRole(com.taskflow.backend.model.Role.USER),
                    com.taskflow.backend.model.NotificationType.TASK_PUBLISHED, "Task published",
                    "“" + updatedTask.getTitle() + "” is now available.", updatedTask);
        }
        return mapToTaskResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        // Safely remove associated assignments before deleting task
        assignmentRepository.deleteAll(assignmentRepository.findByTaskId(id));
        taskRepository.delete(task);
    }

    public TaskResponse mapToTaskResponse(Task task) {
        if (task == null) return null;

        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setInstructions(task.getInstructions());
        response.setDeadline(task.getDeadline());
        response.setProofRequirement(task.getProofRequirement());
        response.setStatus(task.getStatus());
        if (task.getCreatedBy() != null) {
            response.setCreatedById(task.getCreatedBy().getId());
            response.setCreatedByName(task.getCreatedBy().getName());
        }
        response.setAssignedCount(assignmentRepository.countByTaskIdAndIsActiveTrue(task.getId()));
        response.setCompletedCount(assignmentRepository.countByTaskIdAndStatusAndIsActiveTrue(task.getId(), AssignmentStatus.COMPLETED));
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        return response;
    }
}
