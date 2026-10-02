package com.taskflow.backend.service;

import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.AssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class DeadlineReminderService {
    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderService.class);
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public DeadlineReminderService(AssignmentRepository assignmentRepository, NotificationService notificationService, EmailService emailService) {
        this.assignmentRepository = assignmentRepository; this.notificationService = notificationService; this.emailService = emailService;
    }

    @Scheduled(cron = "${taskflow.reminders.cron:0 0 8 * * *}")
    @Transactional
    public void sendDeadlineReminders() {
        LocalDate today = LocalDate.now();
        for (Assignment a : assignmentRepository.findAllActiveWithTaskAndUser()) {
            if (a.getStatus() == AssignmentStatus.COMPLETED || a.getTask() == null || a.getTask().getDeadline() == null) continue;
            LocalDate deadline = a.getTask().getDeadline();
            NotificationType type = null;
            String title = null, message = null;
            if (deadline.equals(today.plusDays(1))) {
                type = NotificationType.DEADLINE_SOON; title = "Task due tomorrow"; message = "“" + a.getTask().getTitle() + "” is due tomorrow.";
            } else if (deadline.isBefore(today)) {
                type = NotificationType.OVERDUE; title = "Task overdue"; message = "“" + a.getTask().getTitle() + "” is overdue. Please update your progress.";
            }
            if (type != null && !notificationService.hasReminder(a.getUser(), a.getTask(), type)) {
                notificationService.create(a.getUser(), type, title, message, a.getTask());
                try { emailService.sendDeadlineReminderEmail(a.getUser().getEmail(), a.getUser().getName(), a.getTask().getTitle(), deadline, type == NotificationType.OVERDUE); }
                catch (Exception ex) { log.warn("Deadline email failed for {}: {}", a.getUser().getEmail(), ex.getMessage()); }
            }
        }
    }
}
