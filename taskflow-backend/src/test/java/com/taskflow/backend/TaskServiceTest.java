package com.taskflow.backend;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.model.Role;
import com.taskflow.backend.model.Task;
import com.taskflow.backend.model.TaskStatus;
import com.taskflow.backend.model.User;
import com.taskflow.backend.repository.AssignmentRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.service.TaskService;
import com.taskflow.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskService taskService;

    private User admin;
    private Task task;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        task = new Task();
        task.setId(10L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setDeadline(LocalDate.now().plusDays(5));
        task.setStatus(TaskStatus.PUBLISHED);
        task.setCreatedBy(admin);
    }

    @Test
    void testGetPublishedTasks() {
        when(taskRepository.findByStatus(TaskStatus.PUBLISHED)).thenReturn(List.of(task));

        List<TaskResponse> responses = taskService.getPublishedTasks();
        assertEquals(1, responses.size());
        assertEquals("Test Task", responses.get(0).getTitle());
        assertEquals(TaskStatus.PUBLISHED, responses.get(0).getStatus());
    }

    @Test
    void testCreateTaskSuccess() {
        TaskRequest request = new TaskRequest("New Task", "Desc", "Instructions", LocalDate.now().plusDays(3), "Proof",
                "PUBLISHED");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(20L);
            return t;
        });

        TaskResponse response = taskService.createTask(request, "admin@example.com");
        assertNotNull(response);
        assertEquals("New Task", response.getTitle());
        assertEquals(TaskStatus.PUBLISHED, response.getStatus());
        assertEquals(admin.getId(), response.getCreatedById());
    }

    @Test
    void testGetTaskByIdNotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(999L));
    }

    @Test
    void testPublishTask() {
        task.setStatus(TaskStatus.DRAFT);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.publishTask(10L);
        assertEquals(TaskStatus.PUBLISHED, response.getStatus());
    }

    @Test
    void testDeleteTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(assignmentRepository.findByTaskId(10L)).thenReturn(List.of());
        doNothing().when(taskRepository).delete(task);

        taskService.deleteTask(10L);
        verify(taskRepository, times(1)).delete(task);
    }
}
