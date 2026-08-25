package com.taskflow.backend.repository;

import com.taskflow.backend.model.Notification;
import com.taskflow.backend.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
    boolean existsByUserIdAndTaskIdAndType(Long userId, Long taskId, NotificationType type);
}
