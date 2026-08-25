package com.taskflow.backend.service;

import com.taskflow.backend.dto.NotificationResponse;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.exception.UnauthorizedAccessException;
import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.NotificationRepository;
import com.taskflow.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationResponse create(User user, NotificationType type, String title, String message, Task task) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setTask(task);
        n.setIsRead(false);
        Notification saved = notificationRepository.save(n);
        NotificationResponse response = map(saved);
        push(user.getEmail(), response);
        return response;
    }

    @Transactional
    public void createForUsers(Collection<User> users, NotificationType type, String title, String message, Task task) {
        for (User user : users) create(user, type, title, message, task);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMine(String email) {
        User user = getUser(email);
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        User user = getUser(email);
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(Long id, String email) {
        User user = getUser(email);
        Notification n = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) throw new UnauthorizedAccessException("You cannot update this notification.");
        n.setIsRead(true);
        return map(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead(String email) {
        User user = getUser(email);
        List<Notification> list = notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId());
        list.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(list);
    }

    public SseEmitter subscribe(String email) {
        getUser(email);
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        emitters.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> emitters.getOrDefault(email, new CopyOnWriteArrayList<>()).remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }

    private void push(String email, NotificationResponse response) {
        List<SseEmitter> list = emitters.get(email);
        if (list == null) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(response));
            } catch (IOException e) {
                emitter.complete();
                list.remove(emitter);
            }
        }
    }

    public boolean hasReminder(User user, Task task, NotificationType type) {
        return notificationRepository.existsByUserIdAndTaskIdAndType(user.getId(), task.getId(), type);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public NotificationResponse map(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId()); r.setType(n.getType()); r.setTitle(n.getTitle()); r.setMessage(n.getMessage());
        r.setIsRead(n.getIsRead()); r.setCreatedAt(n.getCreatedAt());
        if (n.getTask() != null) r.setTaskId(n.getTask().getId());
        return r;
    }
}
