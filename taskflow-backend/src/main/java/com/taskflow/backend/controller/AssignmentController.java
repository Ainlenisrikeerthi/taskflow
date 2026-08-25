package com.taskflow.backend.controller;

import com.taskflow.backend.dto.AssignmentResponse;
import com.taskflow.backend.dto.AssignmentStatusUpdateRequest;
import com.taskflow.backend.exception.BadRequestException;
import com.taskflow.backend.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Assignments", description = "Endpoints for user task self-assignment, progress updates, and proof submissions")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    // View active assignments
    @GetMapping({ "/api/users/me/tasks", "/api/assignments/my" })
    @Operation(summary = "Get current user's active task assignments")
    public ResponseEntity<List<AssignmentResponse>> getMyActiveAssignments(Principal principal) {
        return ResponseEntity.ok(assignmentService.getMyActiveAssignments(principal.getName()));
    }

    // View complete history (active and removed)
    @GetMapping({ "/api/users/me/history", "/api/assignments/my/all" })
    @Operation(summary = "Get current user's complete assignment history")
    public ResponseEntity<List<AssignmentResponse>> getMyAllAssignments(Principal principal) {
        return ResponseEntity.ok(assignmentService.getMyAllAssignments(principal.getName()));
    }

    // Self-assign a task via path
    @PostMapping("/api/tasks/{taskId}/assign")
    @Operation(summary = "Self-assign a published task to current user by task ID")
    public ResponseEntity<AssignmentResponse> assignTaskByPath(
            @PathVariable Long taskId,
            Principal principal) {
        AssignmentResponse response = assignmentService.assignTask(taskId, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Self-assign a task via compatibility route
    @PostMapping("/api/assignments/assign")
    @Operation(summary = "Self-assign a published task to current user (compatibility endpoint)")
    public ResponseEntity<AssignmentResponse> assignTaskCompat(
            @RequestParam(required = false) Long taskId,
            @RequestBody(required = false) Map<String, Object> body,
            Principal principal) {

        Long finalTaskId = taskId;
        if (finalTaskId == null && body != null && body.containsKey("taskId")) {
            Object val = body.get("taskId");
            if (val instanceof Number) {
                finalTaskId = ((Number) val).longValue();
            } else if (val instanceof String) {
                finalTaskId = Long.parseLong((String) val);
            }
        }

        if (finalTaskId == null) {
            throw new BadRequestException("Task ID must be provided via query param taskId or in body");
        }

        AssignmentResponse response = assignmentService.assignTask(finalTaskId, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Self-unassign task by assignment ID
    @DeleteMapping("/api/assignments/{id}")
    @Operation(summary = "Unassign task assignment by assignment ID")
    public ResponseEntity<AssignmentResponse> unassignTask(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(assignmentService.unassignTask(id, principal.getName()));
    }

    @PostMapping("/api/assignments/unassign/{id}")
    @Operation(summary = "Unassign task assignment by ID (POST compatibility alias)")
    public ResponseEntity<AssignmentResponse> unassignTaskPost(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(assignmentService.unassignTask(id, principal.getName()));
    }

    // Self-unassign task by Task ID
    @DeleteMapping("/api/tasks/{taskId}/assignment")
    @Operation(summary = "Unassign active task assignment by Task ID")
    public ResponseEntity<AssignmentResponse> unassignTaskByTaskId(@PathVariable Long taskId, Principal principal) {
        return ResponseEntity.ok(assignmentService.unassignByTaskId(taskId, principal.getName()));
    }

    // Update assignment status & submit proof URL
    @PatchMapping("/api/assignments/{id}/status")
    @Operation(summary = "Update assignment progress status and/or submit proof URL")
    public ResponseEntity<AssignmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentStatusUpdateRequest request,
            Principal principal) {
        return ResponseEntity.ok(assignmentService.updateStatus(id, request, principal.getName()));
    }

    @PutMapping("/api/assignments/update")
    @Operation(summary = "Update assignment progress status and proof URL (PUT compatibility alias)")
    public ResponseEntity<AssignmentResponse> updateAssignmentPut(
            @Valid @RequestBody AssignmentStatusUpdateRequest request,
            Principal principal) {
        if (request.getId() == null) {
            throw new BadRequestException("Assignment ID is required in request body");
        }
        return ResponseEntity.ok(assignmentService.updateStatus(request.getId(), request, principal.getName()));
    }

    // Get specific assignment details
    @GetMapping("/api/assignments/{id}")
    @Operation(summary = "Get assignment details by ID")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id, principal.getName()));
    }

    // Get assignment proof
    @GetMapping("/api/assignments/{id}/proof")
    @Operation(summary = "Get proof URL for an assignment")
    public ResponseEntity<Map<String, String>> getAssignmentProof(@PathVariable Long id, Principal principal) {
        AssignmentResponse assignment = assignmentService.getAssignmentById(id, principal.getName());
        return ResponseEntity.ok(Map.of(
                "assignmentId", String.valueOf(assignment.getId()),
                "proofUrl", assignment.getProofUrl() != null ? assignment.getProofUrl() : ""));
    }
}
