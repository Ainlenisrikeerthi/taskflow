package com.taskflow.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@taskflow.com}")
    private String fromEmail;

    // -------------------------------------------------------------------------
    // Password Reset Email
    // -------------------------------------------------------------------------

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        logger.info("[EMAIL] Attempting to send password reset email to: {}", toEmail);

        String subject = "TaskFlow — Reset Your Password";

        String htmlBody = "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
                + "<h2 style=\"color:#4f46e5;\">TaskFlow</h2>"
                + "<p>Hello <strong>" + escapeHtml(userName) + "</strong>,</p>"
                + "<p>We received a request to reset your password. Click the button below to set a new password:</p>"
                + "<p style=\"text-align:center;margin:32px 0;\">"
                + "<a href=\"" + resetLink + "\" "
                + "style=\"background:#4f46e5;color:#fff;padding:12px 28px;border-radius:6px;"
                + "text-decoration:none;font-weight:bold;display:inline-block;\">Reset Password</a>"
                + "</p>"
                + "<p style=\"font-size:13px;color:#666;\">This link will expire in <strong>1 hour</strong>.</p>"
                + "<p style=\"font-size:13px;color:#666;\">If you did not request a password reset, please ignore this email. "
                + "Your password will remain unchanged.</p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:24px 0;\">"
                + "<p style=\"font-size:12px;color:#999;\">TaskFlow Team</p>"
                + "</div>";

        sendEmail(toEmail, subject, htmlBody, "password reset");
    }

    // -------------------------------------------------------------------------
    // Assignment Removal Email
    // -------------------------------------------------------------------------

    @Override
    public void sendAssignmentRemovalEmail(String toEmail, String userName, String taskTitle,
                                           String currentStatus, String removalReason) {
        logger.info("[EMAIL] Attempting to send assignment removal email to: {}", toEmail);

        String subject = "TaskFlow \u2014 Assignment Removed";

        String reasonSection = (removalReason != null && !removalReason.trim().isEmpty())
                ? "<p><strong>Reason:</strong> " + escapeHtml(removalReason.trim()) + "</p>"
                : "<p><strong>Reason:</strong> No specific reason provided.</p>";

        String htmlBody = "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
                + "<h2 style=\"color:#4f46e5;\">TaskFlow</h2>"
                + "<p>Hello <strong>" + escapeHtml(userName != null ? userName : "User") + "</strong>,</p>"
                + "<p>Your assignment for <strong>\"" + escapeHtml(taskTitle) + "\"</strong> has been removed by an administrator.</p>"
                + reasonSection
                + "<p>You can view the updated status in your TaskFlow history.</p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:24px 0;\">"
                + "<p style=\"font-size:12px;color:#999;\">TaskFlow Team</p>"
                + "</div>";

        // Plain-text fallback
        String textBody = "Hello " + (userName != null ? userName : "User") + ",\n\n"
                + "Your assignment for \"" + taskTitle + "\" has been removed by an administrator.\n\n"
                + "Reason: " + (removalReason != null && !removalReason.trim().isEmpty() ? removalReason.trim() : "No specific reason provided.") + "\n\n"
                + "You can view the updated status in your TaskFlow history.\n\n"
                + "TaskFlow Team";

        logger.info("[EMAIL] Removal notification details — Recipient: {}, Task: \"{}\"", toEmail, taskTitle);
        sendEmail(toEmail, subject, htmlBody, "assignment removal");
    }

    @Override
    public void sendDeadlineReminderEmail(String toEmail, String userName, String taskTitle, java.time.LocalDate deadline, boolean overdue) {
        String subject = overdue ? "TaskFlow — Task Overdue" : "TaskFlow — Task Due Tomorrow";
        String statusLine = overdue ? "is overdue" : "is due tomorrow";
        String htmlBody = "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;\">"
                + "<h2 style=\"color:#6b3f2a;\">TaskFlow</h2>"
                + "<p>Hello <strong>" + escapeHtml(userName) + "</strong>,</p>"
                + "<p>Your task <strong>\"" + escapeHtml(taskTitle) + "\"</strong> " + statusLine + ".</p>"
                + "<p><strong>Deadline:</strong> " + deadline + "</p>"
                + "<p>Please open TaskFlow to review or update your progress.</p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:24px 0;\">"
                + "<p style=\"font-size:12px;color:#999;\">TaskFlow Team</p></div>";
        sendEmail(toEmail, subject, htmlBody, "deadline reminder");
    }

    // -------------------------------------------------------------------------
    // Internal send helper
    // -------------------------------------------------------------------------

    private void sendEmail(String toEmail, String subject, String htmlBody, String operation) {
        if (mailSender == null) {
            logger.error("[EMAIL] JavaMailSender is not configured. Cannot send {} email to {}. "
                    + "Check EMAIL_HOST, EMAIL_PORT, EMAIL_USERNAME, and EMAIL_PASSWORD in your .env file.", operation, toEmail);
            throw new RuntimeException("Email service is not configured. Please check SMTP settings.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            logger.info("[EMAIL] {} email successfully sent to: {}", operation, toEmail);

        } catch (MessagingException e) {
            logger.error("[EMAIL] Failed to build {} email for {}: {}", operation, toEmail, e.getMessage());
            throw new RuntimeException("Failed to send " + operation + " email: " + e.getMessage(), e);
        } catch (MailException e) {
            logger.error("[EMAIL] SMTP error sending {} email to {}. Check SMTP credentials and App Password. Error: {}",
                    operation, toEmail, e.getMessage());
            throw new RuntimeException("Email delivery failed. Please check SMTP configuration (ensure Gmail App Password is used, not your regular password): " + e.getMessage(), e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
