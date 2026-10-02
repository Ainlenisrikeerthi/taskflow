package com.taskflow.backend.controller;

import com.taskflow.backend.dto.CommentRequest;
import com.taskflow.backend.dto.CommentResponse;
import com.taskflow.backend.service.TaskCommentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class TaskCommentController {
    private final TaskCommentService service;
    public TaskCommentController(TaskCommentService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<CommentResponse>> list(@PathVariable Long taskId, Principal principal) { return ResponseEntity.ok(service.list(taskId, principal.getName())); }
    @PostMapping public ResponseEntity<CommentResponse> add(@PathVariable Long taskId, @Valid @RequestBody CommentRequest request, Principal principal) { return new ResponseEntity<>(service.add(taskId, request, principal.getName()), HttpStatus.CREATED); }
    @DeleteMapping("/{commentId}") public ResponseEntity<Void> delete(@PathVariable Long taskId, @PathVariable Long commentId, Principal principal) { service.delete(taskId, commentId, principal.getName()); return ResponseEntity.noContent().build(); }
}
