package com.taskflow.backend.service;

import com.taskflow.backend.dto.CommentRequest;
import com.taskflow.backend.dto.CommentResponse;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.exception.UnauthorizedAccessException;
import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskCommentService {
    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    public TaskCommentService(TaskCommentRepository commentRepository, TaskRepository taskRepository, UserRepository userRepository,
                              AssignmentRepository assignmentRepository, NotificationService notificationService) {
        this.commentRepository = commentRepository; this.taskRepository = taskRepository; this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository; this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId, String email) {
        authorize(taskId, email);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse add(Long taskId, CommentRequest request, String email) {
        Task task = authorize(taskId, email);
        User author = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TaskComment c = new TaskComment(); c.setTask(task); c.setUser(author); c.setMessage(request.getMessage().trim());
        TaskComment saved = commentRepository.save(c);

        if (author.getRole() == Role.ADMIN) {
            List<User> recipients = assignmentRepository.findByTaskIdAndIsActiveTrue(taskId).stream().map(a -> a.getUser()).distinct().collect(Collectors.toList());
            notificationService.createForUsers(recipients, NotificationType.NEW_COMMENT, "New admin comment", author.getName() + " commented on “" + task.getTitle() + "”.", task);
        } else {
            notificationService.createForUsers(userRepository.findByRole(Role.ADMIN), NotificationType.NEW_COMMENT, "New task comment", author.getName() + " commented on “" + task.getTitle() + "”.", task);
        }
        return map(saved);
    }

    @Transactional
    public void delete(Long taskId, Long commentId, String email) {
        authorize(taskId, email);
        User current = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TaskComment c = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!c.getTask().getId().equals(taskId)) throw new ResourceNotFoundException("Comment not found for task");
        if (!c.getUser().getId().equals(current.getId()) && current.getRole() != Role.ADMIN) throw new UnauthorizedAccessException("You can only delete your own comments.");
        commentRepository.delete(c);
    }

    private Task authorize(Long taskId, String email) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) return task;
        if (task.getStatus() != TaskStatus.PUBLISHED && !assignmentRepository.existsByUserIdAndTaskIdAndIsActiveTrue(user.getId(), taskId))
            throw new UnauthorizedAccessException("You cannot access this discussion.");
        return task;
    }

    private CommentResponse map(TaskComment c) {
        CommentResponse r = new CommentResponse(); r.setId(c.getId()); r.setTaskId(c.getTask().getId()); r.setUserId(c.getUser().getId());
        r.setUserName(c.getUser().getName()); r.setUserRole(c.getUser().getRole()); r.setMessage(c.getMessage()); r.setCreatedAt(c.getCreatedAt()); r.setUpdatedAt(c.getUpdatedAt()); return r;
    }
}
