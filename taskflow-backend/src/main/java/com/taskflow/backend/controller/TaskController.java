package com.taskflow.backend.controller;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Tag(name = "Tasks", description = "Endpoints for managing and discovering tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // Public / User endpoints
    @GetMapping("/api/tasks")
    @Operation(summary = "Get all published tasks available for users to assign")
    public ResponseEntity<List<TaskResponse>> getPublishedTasks() {
        return ResponseEntity.ok(taskService.getPublishedTasks());
    }

    @GetMapping("/api/tasks/{id}")
    @Operation(summary = "Get task details by ID")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    // Admin endpoints (supports both /api/admin/tasks and standard REST /api/tasks)
    @GetMapping("/api/admin/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all tasks including drafts (Admin only)")
    public ResponseEntity<List<TaskResponse>> getAllTasksForAdmin() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @PostMapping({"/api/tasks", "/api/admin/tasks"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new task (Admin only)")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request, Principal principal) {
        TaskResponse response = taskService.createTask(request, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping({"/api/tasks/{id}", "/api/admin/tasks/{id}"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing task (Admin only)")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping({"/api/tasks/{id}/publish", "/api/admin/tasks/{id}/publish"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish a task to make it visible to all users (Admin only)")
    public ResponseEntity<TaskResponse> publishTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.publishTask(id));
    }

    @DeleteMapping({"/api/tasks/{id}", "/api/admin/tasks/{id}"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a task and its assignments (Admin only)")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
