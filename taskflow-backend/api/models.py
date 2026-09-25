from django.db import models
from django.utils import timezone


class User(models.Model):
    id = models.BigAutoField(primary_key=True)
    name = models.CharField(max_length=255)
    email = models.CharField(max_length=255, unique=True)
    password_hash = models.CharField(max_length=255, db_column='password_hash')
    google_id = models.CharField(max_length=255, null=True, blank=True, db_column='google_id')
    role = models.CharField(max_length=50, default='USER')
    created_at = models.DateTimeField(null=True, blank=True, db_column='created_at')
    updated_at = models.DateTimeField(null=True, blank=True, db_column='updated_at')

    class Meta:
        db_table = 'users'
        managed = False

    def __str__(self):
        return f"{self.name} <{self.email}> ({self.role})"

    @property
    def is_authenticated(self):
        return True

    @property
    def is_anonymous(self):
        return False

    @property
    def is_admin(self):
        return self.role == 'ADMIN'

    def save(self, *args, **kwargs):
        now = timezone.now()
        if not self.created_at:
            self.created_at = now
        self.updated_at = now
        super().save(*args, **kwargs)


class Task(models.Model):
    id = models.BigAutoField(primary_key=True)
    title = models.CharField(max_length=255)
    description = models.TextField()
    instructions = models.TextField(null=True, blank=True)
    deadline = models.DateField()
    proof_requirement = models.CharField(max_length=255, null=True, blank=True, db_column='proof_requirement')
    status = models.CharField(max_length=50, default='DRAFT')
    created_by = models.ForeignKey(
        User, on_delete=models.SET_NULL, null=True, blank=True, db_column='created_by', related_name='tasks_created'
    )
    task_type = models.CharField(max_length=50, default='GENERAL', db_column='task_type')
    difficulty = models.CharField(max_length=50, null=True, blank=True)
    starter_code = models.TextField(null=True, blank=True, db_column='starter_code')
    created_at = models.DateTimeField(null=True, blank=True, db_column='created_at')
    updated_at = models.DateTimeField(null=True, blank=True, db_column='updated_at')

    class Meta:
        db_table = 'tasks'
        managed = False

    def __str__(self):
        return self.title

    def save(self, *args, **kwargs):
        now = timezone.now()
        if not self.created_at:
            self.created_at = now
        self.updated_at = now
        super().save(*args, **kwargs)


class Assignment(models.Model):
    id = models.BigAutoField(primary_key=True)
    task = models.ForeignKey(Task, on_delete=models.CASCADE, db_column='task_id', related_name='assignments')
    user = models.ForeignKey(User, on_delete=models.CASCADE, db_column='user_id', related_name='assignments')
    status = models.CharField(max_length=50, default='ASSIGNED_NOT_STARTED')
    proof_url = models.CharField(max_length=500, null=True, blank=True, db_column='proof_url')
    is_active = models.BooleanField(default=True, db_column='is_active')
    assigned_at = models.DateTimeField(null=True, blank=True, db_column='assigned_at')
    submitted_at = models.DateTimeField(null=True, blank=True, db_column='submitted_at')
    removed_at = models.DateTimeField(null=True, blank=True, db_column='removed_at')
    removed_reason = models.TextField(null=True, blank=True, db_column='removed_reason')

    class Meta:
        db_table = 'assignments'
        managed = False

    def __str__(self):
        return f"Assignment #{self.id} (Task: {self.task_id}, User: {self.user_id})"

    def save(self, *args, **kwargs):
        if not self.assigned_at:
            self.assigned_at = timezone.now()
        super().save(*args, **kwargs)


class Notification(models.Model):
    id = models.BigAutoField(primary_key=True)
    user = models.ForeignKey(User, on_delete=models.CASCADE, db_column='user_id', related_name='notifications')
    task = models.ForeignKey(Task, on_delete=models.CASCADE, null=True, blank=True, db_column='task_id')
    type = models.CharField(max_length=50)
    title = models.CharField(max_length=180)
    message = models.TextField()
    is_read = models.BooleanField(default=False, db_column='is_read')
    created_at = models.DateTimeField(null=True, blank=True, db_column='created_at')

    class Meta:
        db_table = 'notifications'
        managed = False

    def __str__(self):
        return f"Notification #{self.id} to {self.user_id}: {self.title}"

    def save(self, *args, **kwargs):
        if not self.created_at:
            self.created_at = timezone.now()
        super().save(*args, **kwargs)


class TaskComment(models.Model):
    id = models.BigAutoField(primary_key=True)
    task = models.ForeignKey(Task, on_delete=models.CASCADE, db_column='task_id', related_name='comments')
    user = models.ForeignKey(User, on_delete=models.CASCADE, db_column='user_id', related_name='comments')
    message = models.TextField()
    created_at = models.DateTimeField(null=True, blank=True, db_column='created_at')
    updated_at = models.DateTimeField(null=True, blank=True, db_column='updated_at')

    class Meta:
        db_table = 'task_comments'
        managed = False

    def __str__(self):
        return f"Comment #{self.id} by {self.user_id} on Task {self.task_id}"

    def save(self, *args, **kwargs):
        now = timezone.now()
        if not self.created_at:
            self.created_at = now
        self.updated_at = now
        super().save(*args, **kwargs)


class CodingTestCase(models.Model):
    id = models.BigAutoField(primary_key=True)
    task = models.ForeignKey(Task, on_delete=models.CASCADE, db_column='task_id', related_name='test_cases')
    input = models.TextField()
    expected_output = models.TextField(db_column='expected_output')
    hidden = models.BooleanField(default=False)
    sort_order = models.IntegerField(default=0, db_column='sort_order')

    class Meta:
        db_table = 'coding_test_cases'
        managed = False

    def __str__(self):
        return f"TestCase #{self.id} for Task {self.task_id} (hidden={self.hidden})"


class CodeSubmission(models.Model):
    id = models.BigAutoField(primary_key=True)
    task = models.ForeignKey(Task, on_delete=models.CASCADE, db_column='task_id', related_name='submissions')
    user = models.ForeignKey(User, on_delete=models.CASCADE, db_column='user_id', related_name='submissions')
    language = models.CharField(max_length=50)
    code = models.TextField()
    score = models.FloatField(default=0.0)
    correctness_score = models.FloatField(default=0.0, db_column='correctness_score')
    efficiency_score = models.FloatField(default=0.0, db_column='efficiency_score')
    quality_score = models.FloatField(default=0.0, db_column='quality_score')
    passed_tests = models.IntegerField(default=0, db_column='passed_tests')
    total_tests = models.IntegerField(default=0, db_column='total_tests')
    feedback = models.TextField(null=True, blank=True)
    time_complexity = models.CharField(max_length=100, null=True, blank=True, db_column='time_complexity')
    space_complexity = models.CharField(max_length=100, null=True, blank=True, db_column='space_complexity')
    submitted_at = models.DateTimeField(null=True, blank=True, db_column='submitted_at')

    class Meta:
        db_table = 'code_submissions'
        managed = False

    def __str__(self):
        return f"Submission #{self.id} by {self.user_id} on Task {self.task_id} (score={self.score})"

    def save(self, *args, **kwargs):
        if not self.submitted_at:
            self.submitted_at = timezone.now()
        super().save(*args, **kwargs)


class PasswordResetToken(models.Model):
    id = models.BigAutoField(primary_key=True)
    token = models.CharField(max_length=255, unique=True)
    email = models.CharField(max_length=255)
    expires_at = models.DateTimeField(db_column='expires_at')
    used = models.BooleanField(default=False)
    created_at = models.DateTimeField(null=True, blank=True, db_column='created_at')

    class Meta:
        db_table = 'password_reset_tokens'
        managed = False

    def __str__(self):
        return f"ResetToken for {self.email} (used={self.used})"

    def save(self, *args, **kwargs):
        if not self.created_at:
            self.created_at = timezone.now()
        super().save(*args, **kwargs)
