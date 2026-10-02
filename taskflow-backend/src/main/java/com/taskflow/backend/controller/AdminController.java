package com.taskflow.backend.controller;

import com.taskflow.backend.dto.AssignmentRemoveRequest;
import com.taskflow.backend.dto.AssignmentResponse;
import com.taskflow.backend.dto.DashboardStatsResponse;
import com.taskflow.backend.dto.UserResponse;
import com.taskflow.backend.service.AdminService;
import com.taskflow.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Endpoints for administrative management, monitoring, dashboard statistics, and assignment removal")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get live administrative dashboard summary statistics and recent activity")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/assignments")
    @Operation(summary = "Search and filter assignments by user name/email, status, and task ID")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long taskId) {
        return ResponseEntity.ok(adminService.getAssignments(searchTerm, status, taskId));
    }

    @GetMapping("/tasks/{taskId}/assignments")
    @Operation(summary = "Get all assignments for a specific task")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(adminService.getAssignmentsByTaskId(taskId));
    }

    @GetMapping("/users/{userId}/assignments")
    @Operation(summary = "Get all assignments and task activity for a specific user")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getAssignmentsByUserId(userId));
    }

    @DeleteMapping("/assignments/{id}")
    @Operation(summary = "Remove user assignment at any stage with optional reason and notify via email")
    public ResponseEntity<AssignmentResponse> removeAssignment(
            @PathVariable Long id,
            @RequestBody(required = false) AssignmentRemoveRequest removeRequest,
            Principal principal) {
        String reason = removeRequest != null ? removeRequest.getReason() : null;
        return ResponseEntity.ok(adminService.removeAssignment(id, reason, principal.getName()));
    }

    @PostMapping("/assignments/{id}/remove")
    @Operation(summary = "Remove assignment (POST compatibility alias)")
    public ResponseEntity<AssignmentResponse> removeAssignmentPost(
            @PathVariable Long id,
            @RequestBody(required = false) AssignmentRemoveRequest removeRequest,
            Principal principal) {
        String reason = removeRequest != null ? removeRequest.getReason() : null;
        return ResponseEntity.ok(adminService.removeAssignment(id, reason, principal.getName()));
    }

    @GetMapping("/users")
    @Operation(summary = "Get list of all registered users in the platform")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
