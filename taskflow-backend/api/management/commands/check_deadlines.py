import logging
from datetime import date, timedelta
from django.core.management.base import BaseCommand
from api.models import Assignment, Notification
from api.services import NotificationService, EmailService

logger = logging.getLogger(__name__)


class Command(BaseCommand):
    help = "Checks task deadlines and sends in-app notifications and email reminders"

    def handle(self, *args, **options):
        today = date.today()
        tomorrow = today + timedelta(days=1)
        active_assignments = Assignment.objects.filter(is_active=True).exclude(status='COMPLETED').select_related('task', 'user')

        reminders_sent = 0
        for a in active_assignments:
            if not a.task or not a.task.deadline or not a.user:
                continue

            deadline = a.task.deadline
            n_type = None
            title = None
            message = None

            if deadline == tomorrow:
                n_type = 'DEADLINE_SOON'
                title = "Task due tomorrow"
                message = f"“{a.task.title}” is due tomorrow."
            elif deadline < today:
                n_type = 'OVERDUE'
                title = "Task overdue"
                message = f"“{a.task.title}” is overdue. Please update your progress."

            if n_type:
                # Avoid duplicate reminder
                already_exists = Notification.objects.filter(user=a.user, task=a.task, type=n_type).exists()
                if not already_exists:
                    NotificationService.create(a.user, n_type, title, message, a.task)
                    try:
                        EmailService.send_deadline_reminder_email(
                            a.user.email,
                            a.user.name,
                            a.task.title,
                            deadline,
                            overdue=(n_type == 'OVERDUE')
                        )
                    except Exception as ex:
                        logger.warn("Failed sending deadline email to %s: %s", a.user.email, str(ex))
                    reminders_sent += 1

        self.stdout.write(self.style.SUCCESS(f"Processed deadline checks. Sent {reminders_sent} reminder(s)."))
