package com.taskflow.backend;

import com.taskflow.backend.dto.AssignmentResponse;
import com.taskflow.backend.dto.DashboardStatsResponse;
import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.AssignmentRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.service.AdminService;
import com.taskflow.backend.service.AssignmentService;
import com.taskflow.backend.service.EmailService;
import com.taskflow.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private Task task;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setName("Alice");
        user.setEmail("alice@example.com");

        task = new Task();
        task.setId(10L);
        task.setTitle("Design Mockups");

        assignment = new Assignment();
        assignment.setId(50L);
        assignment.setUser(user);
        assignment.setTask(task);
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setProofUrl("https://figma.com/design");
        assignment.setIsActive(true);
    }

    @Test
    void testGetDashboardStats() {
        when(taskRepository.count()).thenReturn(10L);
        when(userRepository.count()).thenReturn(25L);
        when(assignmentRepository.countByIsActiveTrue()).thenReturn(15L);
        when(assignmentRepository.countByStatusAndIsActiveTrue(AssignmentStatus.COMPLETED)).thenReturn(6L);
        when(assignmentRepository.countByStatusAndIsActiveTrue(AssignmentStatus.STARTED_NOT_COMPLETED)).thenReturn(4L);
        when(assignmentRepository.countByStatusAndIsActiveTrue(AssignmentStatus.ASSIGNED_NOT_STARTED)).thenReturn(5L);
        when(assignmentRepository.findTop10ByOrderByAssignedAtDesc()).thenReturn(List.of(assignment));

        AssignmentResponse dummyResponse = new AssignmentResponse();
        dummyResponse.setId(50L);
        when(assignmentService.mapToAssignmentResponse(assignment)).thenReturn(dummyResponse);

        DashboardStatsResponse stats = adminService.getDashboardStats();
        assertEquals(10L, stats.getTotalTasks());
        assertEquals(25L, stats.getTotalUsers());
        assertEquals(15L, stats.getTotalAssignments());
        assertEquals(6L, stats.getCompletedAssignments());
        assertEquals(4L, stats.getInProgressAssignments());
        assertEquals(5L, stats.getNotStartedAssignments());
        assertEquals(1, stats.getRecentAssignments().size());
    }

    @Test
    void testAdminRemoveAssignmentWorkflow() {
        when(assignmentRepository.findById(50L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);
        AssignmentResponse mockResponse = new AssignmentResponse();
        mockResponse.setStatus(AssignmentStatus.REMOVED);
        when(assignmentService.mapToAssignmentResponse(any(Assignment.class))).thenReturn(mockResponse);

        AssignmentResponse result = adminService.removeAssignment(50L, "Scope changed", "admin@example.com");
        assertEquals(AssignmentStatus.REMOVED, result.getStatus());

        // Verify email sent with reason
        verify(emailService, times(1)).sendAssignmentRemovalEmail(
                eq("alice@example.com"),
                eq("Alice"),
                eq("Design Mockups"),
                eq("COMPLETED"),
                eq("Scope changed"));
    }
}
