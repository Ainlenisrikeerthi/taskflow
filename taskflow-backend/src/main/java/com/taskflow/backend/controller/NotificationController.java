package com.taskflow.backend.controller;

import com.taskflow.backend.dto.NotificationResponse;
import com.taskflow.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }

    @GetMapping public ResponseEntity<List<NotificationResponse>> mine(Principal principal) { return ResponseEntity.ok(notificationService.getMine(principal.getName())); }
    @GetMapping("/unread-count") public ResponseEntity<Map<String, Long>> unread(Principal principal) { return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(principal.getName()))); }
    @PatchMapping("/{id}/read") public ResponseEntity<NotificationResponse> read(@PathVariable Long id, Principal principal) { return ResponseEntity.ok(notificationService.markRead(id, principal.getName())); }
    @PatchMapping("/read-all") public ResponseEntity<Void> readAll(Principal principal) { notificationService.markAllRead(principal.getName()); return ResponseEntity.noContent().build(); }
    @GetMapping(value="/stream", produces="text/event-stream") public SseEmitter stream(Principal principal) { return notificationService.subscribe(principal.getName()); }
}
