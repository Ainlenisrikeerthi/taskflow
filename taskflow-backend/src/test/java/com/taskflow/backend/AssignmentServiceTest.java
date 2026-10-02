package com.taskflow.backend;

import com.taskflow.backend.dto.AssignmentResponse;
import com.taskflow.backend.dto.AssignmentStatusUpdateRequest;
import com.taskflow.backend.exception.BadRequestException;
import com.taskflow.backend.exception.DuplicateResourceException;
import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.AssignmentRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.service.AssignmentService;
import com.taskflow.backend.service.TaskService;
import com.taskflow.backend.service.UserService;
import com.taskflow.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AssignmentService assignmentService;

    private User user;
    private Task task;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setRole(Role.USER);

        task = new Task();
        task.setId(10L);
        task.setTitle("Published Task");
        task.setDescription("Description");
        task.setStatus(TaskStatus.PUBLISHED);
        task.setDeadline(LocalDate.now().plusDays(5));

        assignment = new Assignment();
        assignment.setId(100L);
        assignment.setUser(user);
        assignment.setTask(task);
        assignment.setStatus(AssignmentStatus.ASSIGNED_NOT_STARTED);
        assignment.setIsActive(true);
        assignment.setAssignedAt(LocalDateTime.now());
    }

    @Test
    void testAssignTaskSuccess() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(assignmentRepository.existsByUserIdAndTaskIdAndIsActiveTrue(2L, 10L)).thenReturn(false);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> {
            Assignment a = i.getArgument(0);
            a.setId(100L);
            return a;
        });

        AssignmentResponse response = assignmentService.assignTask(10L, "john@example.com");
        assertNotNull(response);
        assertEquals(AssignmentStatus.ASSIGNED_NOT_STARTED, response.getStatus());
        assertTrue(response.getIsActive());
    }

    @Test
    void testAssignTaskDuplicateActiveFails() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(assignmentRepository.existsByUserIdAndTaskIdAndIsActiveTrue(2L, 10L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> assignmentService.assignTask(10L, "john@example.com"));
    }

    @Test
    void testAssignDraftTaskFails() {
        task.setStatus(TaskStatus.DRAFT);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThrows(BadRequestException.class, () -> assignmentService.assignTask(10L, "john@example.com"));
    }

    @Test
    void testUnassignTask() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

        AssignmentResponse response = assignmentService.unassignTask(100L, "john@example.com");
        assertFalse(response.getIsActive());
        assertEquals(AssignmentStatus.REMOVED, response.getStatus());
        assertNotNull(response.getRemovedAt());
    }

    @Test
    void testUpdateStatusToInProgress() {
        AssignmentStatusUpdateRequest request = new AssignmentStatusUpdateRequest(100L, "STARTED_NOT_COMPLETED", null);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

        AssignmentResponse response = assignmentService.updateStatus(100L, request, "john@example.com");
        assertEquals(AssignmentStatus.STARTED_NOT_COMPLETED, response.getStatus());
    }

    @Test
    void testCompleteTaskWithoutProofUrlFails() {
        AssignmentStatusUpdateRequest request = new AssignmentStatusUpdateRequest(100L, "COMPLETED", null);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> assignmentService.updateStatus(100L, request, "john@example.com"));
        assertTrue(ex.getMessage().contains("Proof URL is required"));
    }

    @Test
    void testCompleteTaskWithValidProofUrlSuccess() {
        AssignmentStatusUpdateRequest request = new AssignmentStatusUpdateRequest(100L, "COMPLETED",
                "https://github.com/my-work");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

        AssignmentResponse response = assignmentService.updateStatus(100L, request, "john@example.com");
        assertEquals(AssignmentStatus.COMPLETED, response.getStatus());
        assertEquals("https://github.com/my-work", response.getProofUrl());
        assertNotNull(response.getSubmittedAt());
    }

    @Test
    void testCompleteTaskWithInvalidUrlFails() {
        AssignmentStatusUpdateRequest request = new AssignmentStatusUpdateRequest(100L, "COMPLETED", "not-a-valid-url");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThrows(BadRequestException.class,
                () -> assignmentService.updateStatus(100L, request, "john@example.com"));
    }
}
