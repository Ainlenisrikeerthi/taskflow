import json
import uuid
import time
from datetime import datetime, timedelta, timezone as dt_tz
from django.utils import timezone
from django.http import StreamingHttpResponse
from django.conf import settings
from rest_framework.decorators import api_view, permission_classes, authentication_classes
from rest_framework.response import Response
from rest_framework import status
from rest_framework.exceptions import PermissionDenied

from .models import (
    User, Task, Assignment, Notification, TaskComment,
    CodingTestCase, CodeSubmission, PasswordResetToken
)
from .security import check_password, hash_password, generate_jwt
from .authentication import IsAuthenticated, IsAdmin, IsUser
from .serializers import (
    serialize_user, serialize_task, serialize_assignment,
    serialize_notification, serialize_comment, serialize_test_case,
    serialize_submission
)
from .exceptions import (
    ResourceNotFoundException, BadRequestException,
    DuplicateResourceException, UnauthorizedAccessException
)
from .services import (
    NotificationService, EmailService, CodeExecutionService,
    CodingAgentService, sse_subscribe, sse_unsubscribe
)


# -----------------------------------------------------------------------------
# 1. AUTHENTICATION ENDPOINTS
# -----------------------------------------------------------------------------

@api_view(['POST'])
def login_view(request):
    data = request.data or {}
    email = (data.get('email') or '').strip().lower()
    raw_password = data.get('password') or ''

    if not email or not raw_password:
        raise BadRequestException("Email and password are required")

    user = User.objects.filter(email=email).first()
    if not user or not check_password(raw_password, user.password_hash):
        return Response({
            "status": 401,
            "error": "Unauthorized",
            "message": "Invalid email or password",
            "path": request.path
        }, status=status.HTTP_401_UNAUTHORIZED)

    jwt_token = generate_jwt(user.email)
    return Response({
        "token": jwt_token,
        "type": "Bearer",
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "role": user.role
    })


@api_view(['POST'])
def register_view(request):
    data = request.data or {}
    name = (data.get('name') or '').strip()
    email = (data.get('email') or '').strip().lower()
    raw_password = data.get('password') or ''

    if not name or not email or not raw_password:
        raise BadRequestException("Name, email, and password are required")

    if User.objects.filter(email=email).exists():
        raise DuplicateResourceException(f"Email is already registered: {email}")

    if len(raw_password) < 6:
        raise BadRequestException("Password must be at least 6 characters long")

    user = User.objects.create(
        name=name,
        email=email,
        password_hash=hash_password(raw_password),
        role='USER'  # Public registration always creates USER
    )

    jwt_token = generate_jwt(user.email)
    return Response({
        "token": jwt_token,
        "type": "Bearer",
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "role": user.role
    }, status=status.HTTP_201_CREATED)


@api_view(['POST'])
def google_login_view(request):
    data = request.data or {}
    google_id = data.get('googleId')
    email = (data.get('email') or '').strip().lower()
    name = (data.get('name') or 'Google User').strip()

    if not google_id or not email:
        raise BadRequestException("Google ID and email are required")

    user = User.objects.filter(google_id=google_id).first()
    if not user:
        user = User.objects.filter(email=email).first()
        if user:
            user.google_id = google_id
            user.save()
        else:
            user = User.objects.create(
                name=name,
                email=email,
                password_hash=hash_password(str(uuid.uuid4())),
                google_id=google_id,
                role='USER'
            )

    jwt_token = generate_jwt(user.email)
    return Response({
        "token": jwt_token,
        "type": "Bearer",
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "role": user.role
    })


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def auth_me_view(request):
    return Response(serialize_user(request.user))


@api_view(['POST'])
def forgot_password_view(request):
    data = request.data or {}
    email = (data.get('email') or '').strip().lower()
    if not email:
        raise BadRequestException("Email is required")

    user = User.objects.filter(email=email).first()
    if not user:
        raise ResourceNotFoundException("No TaskFlow account was found with that email address.")

    # Remove existing tokens for this email
    PasswordResetToken.objects.filter(email=email).delete()

    raw_token = str(uuid.uuid4())
    expires_at = timezone.now() + timedelta(hours=1)
    PasswordResetToken.objects.create(
        token=raw_token,
        email=email,
        expires_at=expires_at,
        used=False
    )

    frontend_url = settings.FRONTEND_URL.rstrip('/')
    reset_link = f"{frontend_url}/reset-password?token={raw_token}"
    EmailService.send_password_reset_email(email, user.name, reset_link)

    return Response({
        "message": "Password reset email sent successfully.",
        "delivered": True
    })


@api_view(['POST'])
def reset_password_view(request):
    data = request.data or {}
    token = data.get('token')
    new_password = data.get('newPassword')

    if not token or not new_password:
        raise BadRequestException("Token and new password are required")

    if len(new_password) < 6:
        raise BadRequestException("New password must be at least 6 characters long.")

    reset_token = PasswordResetToken.objects.filter(token=token).first()
    if not reset_token:
        raise BadRequestException("Invalid or expired password reset link. Please request a new one.")

    if reset_token.used:
        raise BadRequestException("This password reset link has already been used. Please request a new one.")

    if reset_token.expires_at < timezone.now():
        reset_token.delete()
        raise BadRequestException("This password reset link has expired (valid for 1 hour). Please request a new one.")

    user = User.objects.filter(email=reset_token.email).first()
    if not user:
        raise ResourceNotFoundException("User not found for this reset token.")

    user.password_hash = hash_password(new_password)
    user.save()

    reset_token.used = True
    reset_token.save()

    return Response({
        "message": "Your password has been reset successfully. You can now sign in with your new password."
    })


# -----------------------------------------------------------------------------
# 2. USER PROFILE ENDPOINTS
# -----------------------------------------------------------------------------

@api_view(['GET', 'PUT'])
@permission_classes([IsAuthenticated])
def user_me_view(request):
    if request.method == 'GET':
        return Response(serialize_user(request.user))
    
    # PUT
    name = (request.data.get('name') or '').strip()
    if not name:
        raise BadRequestException("Name cannot be empty")
    request.user.name = name
    request.user.save()
    return Response(serialize_user(request.user))


@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdmin])
def user_by_id_view(request, user_id):
    user = User.objects.filter(id=user_id).first()
    if not user:
        raise ResourceNotFoundException(f"User not found with id: {user_id}")
    return Response(serialize_user(user))


@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdmin])
def admin_all_users_view(request):
    users = User.objects.all().order_by('id')
    return Response([serialize_user(u) for u in users])


# -----------------------------------------------------------------------------
# 3. TASK ENDPOINTS
# -----------------------------------------------------------------------------

@api_view(['GET'])
def get_published_tasks_view(request):
    tasks = Task.objects.filter(status='PUBLISHED').order_by('-id')
    return Response([serialize_task(t) for t in tasks])


@api_view(['GET'])
def get_task_by_id_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException(f"Task not found with id: {task_id}")
    return Response(serialize_task(task))


@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdmin])
def admin_all_tasks_view(request):
    tasks = Task.objects.all().order_by('-id')
    return Response([serialize_task(t) for t in tasks])


@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdmin])
def create_task_view(request):
    data = request.data or {}
    title = (data.get('title') or '').strip()
    description = (data.get('description') or '').strip()
    instructions = data.get('instructions')
    deadline_str = data.get('deadline')
    proof_req = data.get('proofRequirement')
    status_val = (data.get('status') or 'DRAFT').upper()
    task_type = (data.get('taskType') or 'GENERAL').upper()
    difficulty = data.get('difficulty')
    starter_code = data.get('starterCode')

    if not title or not description or not deadline_str:
        raise BadRequestException("Title, description, and deadline are required")

    try:
        deadline = datetime.strptime(deadline_str, "%Y-%m-%d").date()
    except ValueError:
        raise BadRequestException("Deadline must be in YYYY-MM-DD format")

    task = Task.objects.create(
        title=title,
        description=description,
        instructions=instructions,
        deadline=deadline,
        proof_requirement=proof_req,
        status=status_val if status_val in ('DRAFT', 'PUBLISHED') else 'DRAFT',
        task_type=task_type,
        difficulty=difficulty,
        starter_code=starter_code,
        created_by=request.user
    )

    if task.status == 'PUBLISHED':
        regular_users = User.objects.filter(role='USER')
        NotificationService.create_for_users(
            regular_users,
            'TASK_PUBLISHED',
            'New task published',
            f"A new task “{task.title}” is now available.",
            task
        )

    return Response(serialize_task(task), status=status.HTTP_201_CREATED)


@api_view(['PUT'])
@permission_classes([IsAuthenticated, IsAdmin])
def update_task_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException(f"Task not found with id: {task_id}")

    prev_status = task.status
    data = request.data or {}

    if 'title' in data and data['title'].strip():
        task.title = data['title'].strip()
    if 'description' in data and data['description'].strip():
        task.description = data['description'].strip()
    if 'instructions' in data:
        task.instructions = data['instructions']
    if 'deadline' in data and data['deadline']:
        try:
            task.deadline = datetime.strptime(data['deadline'], "%Y-%m-%d").date()
        except ValueError:
            raise BadRequestException("Deadline must be in YYYY-MM-DD format")
    if 'proofRequirement' in data:
        task.proof_requirement = data['proofRequirement']
    if 'taskType' in data and data['taskType']:
        task.task_type = data['taskType'].upper()
    if 'difficulty' in data:
        task.difficulty = data['difficulty']
    if 'starterCode' in data:
        task.starter_code = data['starterCode']
    if 'status' in data and data['status']:
        st = data['status'].upper()
        if st not in ('DRAFT', 'PUBLISHED'):
            raise BadRequestException(f"Invalid task status: {data['status']}")
        task.status = st

    task.save()

    if task.status == 'PUBLISHED':
        if prev_status != 'PUBLISHED':
            regular_users = User.objects.filter(role='USER')
            NotificationService.create_for_users(
                regular_users, 'TASK_PUBLISHED', 'Task published',
                f"“{task.title}” is now available.", task
            )
        else:
            recipients = [a.user for a in Assignment.objects.filter(task_id=task.id, is_active=True).select_related('user')]
            NotificationService.create_for_users(
                recipients, 'TASK_UPDATED', 'Task updated',
                f"“{task.title}” has been updated.", task
            )

    return Response(serialize_task(task))


@api_view(['PATCH'])
@permission_classes([IsAuthenticated, IsAdmin])
def publish_task_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException(f"Task not found with id: {task_id}")

    was_published = task.status == 'PUBLISHED'
    task.status = 'PUBLISHED'
    task.save()

    if not was_published:
        regular_users = User.objects.filter(role='USER')
        NotificationService.create_for_users(
            regular_users, 'TASK_PUBLISHED', 'Task published',
            f"“{task.title}” is now available.", task
        )

    return Response(serialize_task(task))


@api_view(['DELETE'])
@permission_classes([IsAuthenticated, IsAdmin])
def delete_task_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException(f"Task not found with id: {task_id}")

    CodeSubmission.objects.filter(task_id=task_id).delete()
    CodingTestCase.objects.filter(task_id=task_id).delete()
    Assignment.objects.filter(task_id=task_id).delete()
    task.delete()
    return Response(status=status.HTTP_204_NO_CONTENT)


# -----------------------------------------------------------------------------
# 4. ASSIGNMENT ENDPOINTS
# -----------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def my_active_assignments_view(request):
    assignments = Assignment.objects.filter(user=request.user, is_active=True).order_by('-assigned_at')
    return Response([serialize_assignment(a) for a in assignments])


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def my_all_assignments_view(request):
    assignments = Assignment.objects.filter(user=request.user).order_by('-assigned_at')
    return Response([serialize_assignment(a) for a in assignments])


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def assign_task_view(request, task_id=None):
    if task_id is None:
        # Compatibility route: /api/assignments/assign?taskId=X or in body
        task_id = request.query_params.get('taskId')
        if not task_id and request.data:
            task_id = request.data.get('taskId')

    if not task_id:
        raise BadRequestException("Task ID must be provided via path, query param taskId, or in body")

    try:
        task_id = int(task_id)
    except (ValueError, TypeError):
        raise BadRequestException("Invalid task ID")

    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException(f"Task not found with id: {task_id}")

    if task.status != 'PUBLISHED':
        raise BadRequestException("Cannot assign a task that is not published.")

    # Check for duplicate active assignment
    if Assignment.objects.filter(user=request.user, task=task, is_active=True).exists():
        raise DuplicateResourceException("You already have an active assignment for this task.")

    assignment = Assignment.objects.create(
        task=task,
        user=request.user,
        status='ASSIGNED_NOT_STARTED',
        is_active=True
    )

    NotificationService.create(
        request.user, 'ASSIGNMENT_CREATED', 'Task assigned',
        f"You assigned yourself to “{task.title}”.", task
    )

    return Response(serialize_assignment(assignment), status=status.HTTP_201_CREATED)


@api_view(['DELETE', 'POST'])
@permission_classes([IsAuthenticated])
def unassign_by_id_view(request, assignment_id):
    assignment = Assignment.objects.filter(id=assignment_id).first()
    if not assignment:
        raise ResourceNotFoundException(f"Assignment not found with id: {assignment_id}")

    if assignment.user_id != request.user.id and not request.user.is_admin:
        raise UnauthorizedAccessException("You can only unassign your own tasks.")

    if not assignment.is_active:
        raise BadRequestException("This assignment is already inactive or removed.")

    assignment.is_active = False
    assignment.status = 'REMOVED'
    assignment.removed_at = timezone.now()
    assignment.removed_reason = "Self-unassigned by user"
    assignment.save()

    NotificationService.create(
        assignment.user, 'ASSIGNMENT_REMOVED', 'Task unassigned',
        f"Your assignment for “{assignment.task.title}” was removed.", assignment.task
    )

    return Response(serialize_assignment(assignment))


@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
def unassign_by_task_id_view(request, task_id):
    assignment = Assignment.objects.filter(user=request.user, task_id=task_id, is_active=True).first()
    if not assignment:
        raise ResourceNotFoundException(f"No active assignment found for task id: {task_id}")

    return unassign_by_id_view(request, assignment.id)


def normalize_assignment_status(status_str):
    if not status_str or not status_str.strip():
        raise BadRequestException("Status cannot be empty.")
    norm = status_str.strip().upper()
    if norm in ('IN_PROGRESS', 'STARTED'):
        return 'STARTED_NOT_COMPLETED'
    if norm in ('ASSIGNED', 'NOT_STARTED'):
        return 'ASSIGNED_NOT_STARTED'
    if norm in ('ASSIGNED_NOT_STARTED', 'STARTED_NOT_COMPLETED', 'COMPLETED', 'REMOVED'):
        return norm
    raise BadRequestException(f"Invalid assignment status: {status_str}")


@api_view(['PATCH', 'PUT'])
@permission_classes([IsAuthenticated])
def update_assignment_status_view(request, assignment_id=None):
    if assignment_id is None:
        # PUT /api/assignments/update compatibility alias
        assignment_id = request.data.get('id')
        if not assignment_id:
            raise BadRequestException("Assignment ID is required in request body")

    assignment = Assignment.objects.filter(id=assignment_id).first()
    if not assignment:
        raise ResourceNotFoundException(f"Assignment not found with id: {assignment_id}")

    if assignment.user_id != request.user.id and not request.user.is_admin:
        raise UnauthorizedAccessException("You can only update your own assignments.")

    if not assignment.is_active:
        raise BadRequestException("Cannot update an inactive or removed assignment.")

    data = request.data or {}
    raw_status = data.get('status')
    proof_url = data.get('proofUrl')

    target_status = normalize_assignment_status(raw_status) if raw_status else assignment.status

    if target_status == 'COMPLETED':
        if not proof_url or not proof_url.strip():
            raise BadRequestException("Proof URL is required when completing a task.")
        url_str = proof_url.strip()
        if not (url_str.startswith('http://') or url_str.startswith('https://')):
            raise BadRequestException("Proof URL must be a valid HTTP or HTTPS URL.")
        assignment.proof_url = url_str
        assignment.submitted_at = timezone.now()
    else:
        if proof_url is not None:
            assignment.proof_url = proof_url.strip()

    prev_status = assignment.status
    assignment.status = target_status
    assignment.save()

    if target_status == 'COMPLETED' and prev_status != 'COMPLETED':
        NotificationService.create(
            assignment.user, 'TASK_COMPLETED', 'Task completed',
            f"You completed “{assignment.task.title}”.", assignment.task
        )
        admins = User.objects.filter(role='ADMIN')
        NotificationService.create_for_users(
            admins, 'TASK_COMPLETED', 'Task completed',
            f"{assignment.user.name} completed “{assignment.task.title}”.", assignment.task
        )

    return Response(serialize_assignment(assignment))


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_assignment_by_id_view(request, assignment_id):
    assignment = Assignment.objects.filter(id=assignment_id).first()
    if not assignment:
        raise ResourceNotFoundException(f"Assignment not found with id: {assignment_id}")

    if assignment.user_id != request.user.id and not request.user.is_admin:
        raise UnauthorizedAccessException("You are not authorized to view this assignment.")

    return Response(serialize_assignment(assignment))


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_assignment_proof_view(request, assignment_id):
    assignment = Assignment.objects.filter(id=assignment_id).first()
    if not assignment:
        raise ResourceNotFoundException(f"Assignment not found with id: {assignment_id}")

    if assignment.user_id != request.user.id and not request.user.is_admin:
        raise UnauthorizedAccessException("You are not authorized to view this assignment.")

    return Response({
        "assignmentId": str(assignment.id),
        "proofUrl": assignment.proof_url or ""
    })


# -----------------------------------------------------------------------------
# 5. ADMIN ENDPOINTS
# -----------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdmin])
def admin_dashboard_view(request):
    total_tasks = Task.objects.count()
    total_users = User.objects.count()
    total_assignments = Assignment.objects.filter(is_active=True).count()
    completed_assignments = Assignment.objects.filter(status='COMPLETED', is_active=True).count()
    in_progress_assignments = Assignment.objects.filter(status='STARTED_NOT_COMPLETED', is_active=True).count()
    not_started_assignments = Assignment.objects.filter(status='ASSIGNED_NOT_STARTED', is_active=True).count()

    recent_assignments = Assignment.objects.order_by('-assigned_at')[:10]

    return Response({
        "totalTasks": total_tasks,
        "totalUsers": total_users,
        "totalAssignments": total_assignments,
        "completedAssignments": completed_assignments,
        "inProgressAssignments": in_progress_assignments,
        "notStartedAssignments": not_started_assignments,
        "recentAssignments": [serialize_assignment(a) for a in recent_assignments]
    })


@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdmin])
def admin_assignments_view(request):
    search_term = request.query_params.get('searchTerm', '').strip()
    status_param = request.query_params.get('status', '').strip()
    task_id = request.query_params.get('taskId', '').strip()

    qs = Assignment.objects.all().select_related('task', 'user').order_by('-assigned_at')

    if task_id:
        try:
            qs = qs.filter(task_id=int(task_id))
        except ValueError:
            pass

    if status_param:
        try:
            norm_status = normalize_assignment_status(status_param)
            qs = qs.filter(status=norm_status)
        except Exception:
            pass

    if search_term:
        qs = qs.filter(
            models_or(
                user__name__icontains=search_term,
                user__email__icontains=search_term,
                task__title__icontains=search_term
            )
        )

    return Response([serialize_assignment(a) for a in qs])


def models_or(*queries):
    from django.db.models import Q
    res = Q()
    for q in queries:
        res |= Q(**q) if isinstance(q, dict) else q
    return res


@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdmin])
def admin_task_assignments_view(request, task_id):
    assignments = Assignment.objects.filter(task_id=task_id).order_by('-assigned_at')
    return Response([serialize_assignment(a) for a in assignments])


@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdmin])
def admin_user_assignments_view(request, user_id):
    assignments = Assignment.objects.filter(user_id=user_id).order_by('-assigned_at')
    return Response([serialize_assignment(a) for a in assignments])


@api_view(['DELETE', 'POST'])
@permission_classes([IsAuthenticated, IsAdmin])
def admin_remove_assignment_view(request, assignment_id):
    assignment = Assignment.objects.filter(id=assignment_id).first()
    if not assignment:
        raise ResourceNotFoundException(f"Assignment not found with id: {assignment_id}")

    data = request.data or {}
    reason = data.get('reason')
    reason_clean = reason.strip() if reason and reason.strip() else None

    original_status = assignment.status
    task_title = assignment.task.title if assignment.task else "Task"
    user_email = assignment.user.email if assignment.user else None
    user_name = assignment.user.name if assignment.user else "User"

    assignment.is_active = False
    assignment.status = 'REMOVED'
    assignment.removed_at = timezone.now()
    assignment.removed_reason = reason_clean
    assignment.save()

    if assignment.user:
        NotificationService.create(
            assignment.user, 'ASSIGNMENT_REMOVED', 'Assignment removed',
            f"Your assignment for “{task_title}” was removed by an administrator.", assignment.task
        )

    if user_email:
        EmailService.send_assignment_removal_email(
            user_email, user_name, task_title, original_status, reason_clean
        )

    return Response(serialize_assignment(assignment))


# -----------------------------------------------------------------------------
# 6. TASK COMMENTS / DISCUSSIONS
# -----------------------------------------------------------------------------

@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def task_comments_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException(f"Task not found with id: {task_id}")

    # Authorization check
    if not request.user.is_admin and task.status != 'PUBLISHED':
        has_assign = Assignment.objects.filter(task=task, user=request.user, is_active=True).exists()
        if not has_assign:
            raise UnauthorizedAccessException("You cannot access this discussion.")

    if request.method == 'GET':
        comments = TaskComment.objects.filter(task=task).select_related('user').order_by('created_at')
        return Response([serialize_comment(c) for c in comments])

    # POST: Add comment
    message = (request.data.get('message') or '').strip()
    if not message:
        raise BadRequestException("Message cannot be empty")

    comment = TaskComment.objects.create(
        task=task,
        user=request.user,
        message=message
    )

    if request.user.is_admin:
        assigned_users = [a.user for a in Assignment.objects.filter(task=task, is_active=True).select_related('user')]
        NotificationService.create_for_users(
            assigned_users, 'NEW_COMMENT', 'New admin comment',
            f"{request.user.name} commented on “{task.title}”.", task
        )
    else:
        admins = User.objects.filter(role='ADMIN')
        NotificationService.create_for_users(
            admins, 'NEW_COMMENT', 'New task comment',
            f"{request.user.name} commented on “{task.title}”.", task
        )

    return Response(serialize_comment(comment), status=status.HTTP_201_CREATED)


@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
def delete_task_comment_view(request, task_id, comment_id):
    comment = TaskComment.objects.filter(id=comment_id, task_id=task_id).first()
    if not comment:
        raise ResourceNotFoundException("Comment not found for task")

    if comment.user_id != request.user.id and not request.user.is_admin:
        raise UnauthorizedAccessException("You can only delete your own comments.")

    comment.delete()
    return Response(status=status.HTTP_204_NO_CONTENT)


# -----------------------------------------------------------------------------
# 7. IN-APP NOTIFICATIONS & SSE STREAM
# -----------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def my_notifications_view(request):
    notifications = Notification.objects.filter(user=request.user).order_by('-created_at')[:50]
    return Response([serialize_notification(n) for n in notifications])


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def unread_notifications_count_view(request):
    count = Notification.objects.filter(user=request.user, is_read=False).count()
    return Response({"count": count})


@api_view(['PATCH'])
@permission_classes([IsAuthenticated])
def mark_notification_read_view(request, notification_id):
    n = Notification.objects.filter(id=notification_id).first()
    if not n:
        raise ResourceNotFoundException("Notification not found")

    if n.user_id != request.user.id:
        raise UnauthorizedAccessException("You cannot update this notification.")

    n.is_read = True
    n.save()
    return Response(serialize_notification(n))


@api_view(['PATCH'])
@permission_classes([IsAuthenticated])
def mark_all_notifications_read_view(request):
    Notification.objects.filter(user=request.user, is_read=False).update(is_read=True)
    return Response(status=status.HTTP_204_NO_CONTENT)


def sse_event_stream(email):
    q = sse_subscribe(email)
    yield "event: connected\ndata: ok\n\n"
    try:
        while True:
            try:
                item = q.get(timeout=25)
                ev = item.get('event', 'notification')
                data = json.dumps(item.get('data', {}))
                yield f"event: {ev}\ndata: {data}\n\n"
            except queue.Empty:
                # Keep-alive heartbeat comment
                yield ": keepalive\n\n"
    finally:
        sse_unsubscribe(email, q)


def notification_stream_view(request):
    # Authenticate token from query or Authorization header
    token = None
    auth_header = request.headers.get('Authorization')
    if auth_header and auth_header.startswith('Bearer '):
        token = auth_header.split(' ')[1].strip()
    elif 'token' in request.GET:
        token = request.GET.get('token')

    if not token:
        return Response({"message": "Unauthorized"}, status=401)

    from .security import decode_jwt
    try:
        email = decode_jwt(token)
    except Exception:
        return Response({"message": "Invalid token"}, status=401)

    response = StreamingHttpResponse(
        sse_event_stream(email),
        content_type='text/event-stream'
    )
    response['Cache-Control'] = 'no-cache'
    response['X-Accel-Buffering'] = 'no'
    return response


# -----------------------------------------------------------------------------
# 8. CODING LAB / DSA ASSESSMENT AGENT ENDPOINTS
# -----------------------------------------------------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdmin])
def coding_generate_view(request):
    data = request.data or {}
    title = data.get('title')
    difficulty = data.get('difficulty')
    language = data.get('language')
    result = CodingAgentService.generate(title, difficulty, language)
    return Response(result)


@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdmin])
def coding_save_task_view(request):
    data = request.data or {}
    title = (data.get('title') or '').strip()
    description = (data.get('description') or '').strip()
    instructions = data.get('instructions')
    deadline_str = data.get('deadline')
    status_val = (data.get('status') or 'DRAFT').upper()
    difficulty = data.get('difficulty')
    starter_code = data.get('starterCode')
    test_cases_data = data.get('testCases') or []

    if not title:
        raise BadRequestException("Problem title is required")
    if not description:
        raise BadRequestException("Problem description is required")
    if not deadline_str:
        raise BadRequestException("Deadline is required")
    if not test_cases_data:
        raise BadRequestException("Add at least one test case before saving")

    for i, tc in enumerate(test_cases_data):
        inp = (tc.get('input') or '').strip()
        out = (tc.get('expectedOutput') or '').strip()
        if not inp or not out:
            raise BadRequestException(f"Test case {i+1} needs both input and expected output")
        if inp.lower().startswith("edit_") or out.lower().startswith("edit_"):
            raise BadRequestException(f"Test case {i+1} still contains a placeholder. Review before saving.")

    try:
        deadline = datetime.strptime(deadline_str, "%Y-%m-%d").date()
    except ValueError:
        raise BadRequestException("Deadline must be in YYYY-MM-DD format")

    task = Task.objects.create(
        title=title,
        description=description,
        instructions=instructions,
        deadline=deadline,
        proof_requirement="Code submission required",
        status=status_val if status_val in ('DRAFT', 'PUBLISHED') else 'DRAFT',
        task_type='CODING',
        difficulty=difficulty,
        starter_code=starter_code,
        created_by=request.user
    )

    # Save test cases
    for idx, tc in enumerate(test_cases_data):
        CodingTestCase.objects.create(
            task=task,
            input=tc.get('input', '').strip(),
            expected_output=tc.get('expectedOutput', '').strip(),
            hidden=bool(tc.get('hidden', False)),
            sort_order=idx
        )

    if task.status == 'PUBLISHED':
        regular_users = User.objects.filter(role='USER')
        NotificationService.create_for_users(
            regular_users, 'TASK_PUBLISHED', 'New task published',
            f"A new task “{task.title}” is now available.", task
        )

    return Response(serialize_task(task), status=status.HTTP_201_CREATED)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_visible_tests_view(request, task_id):
    tests = CodingTestCase.objects.filter(task_id=task_id, hidden=False).order_by('sort_order', 'id')
    return Response([serialize_test_case(t, include_hidden=False) for t in tests])


@api_view(['PUT'])
@permission_classes([IsAuthenticated, IsAdmin])
def save_tests_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException("Task not found")

    test_cases_data = request.data or []
    CodingTestCase.objects.filter(task_id=task_id).delete()

    for idx, tc in enumerate(test_cases_data):
        CodingTestCase.objects.create(
            task=task,
            input=tc.get('input', '').strip(),
            expected_output=tc.get('expectedOutput', '').strip(),
            hidden=bool(tc.get('hidden', False)),
            sort_order=idx
        )

    all_tests = CodingTestCase.objects.filter(task_id=task_id).order_by('sort_order', 'id')
    return Response([serialize_test_case(t, include_hidden=True) for t in all_tests])


@api_view(['POST'])
@permission_classes([IsAuthenticated, IsUser])
def coding_run_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException("Task not found")
    if task.task_type != 'CODING':
        raise BadRequestException("This is not a coding task")

    # Check assignment
    has_assignment = Assignment.objects.filter(task=task, user=request.user, is_active=True).exists()
    if not has_assignment:
        raise BadRequestException("Assign this coding task before running or submitting code")

    data = request.data or {}
    language = data.get('language')
    code = data.get('code')
    if not code or not code.strip():
        raise BadRequestException("Write some code before running tests")

    visible_tests = CodingTestCase.objects.filter(task_id=task_id, hidden=False).order_by('sort_order', 'id')
    if not visible_tests.exists():
        raise BadRequestException("No visible test cases configured by admin")

    cases_results = []
    passed = 0
    case_num = 1

    for tc in visible_tests:
        res = CodeExecutionService.run(language, code, tc.input)
        is_passed = (res["exitCode"] == 0 and 
                     CodingAgentService.normalize_str(res["stdout"]) == CodingAgentService.normalize_str(tc.expected_output))
        if is_passed:
            passed += 1

        cases_results.append({
            "caseNumber": case_num,
            "input": tc.input,
            "expectedOutput": tc.expected_output,
            "actualOutput": res["stdout"],
            "passed": is_passed,
            "error": res["stderr"]
        })
        case_num += 1

    return Response({
        "passed": passed,
        "total": len(visible_tests),
        "cases": cases_results
    })


@api_view(['POST'])
@permission_classes([IsAuthenticated, IsUser])
def coding_submit_view(request, task_id):
    task = Task.objects.filter(id=task_id).first()
    if not task:
        raise ResourceNotFoundException("Task not found")
    if task.task_type != 'CODING':
        raise BadRequestException("This is not a coding task")

    assignment = Assignment.objects.filter(task=task, user=request.user, is_active=True).first()
    if not assignment:
        raise BadRequestException("Assign this coding task before running or submitting code")

    data = request.data or {}
    language = data.get('language')
    code = data.get('code')
    if not code or not code.strip():
        raise BadRequestException("Write some code before submitting")

    all_tests = list(CodingTestCase.objects.filter(task_id=task_id).order_by('sort_order', 'id'))
    if not all_tests:
        raise BadRequestException("No test cases configured by admin")

    eval_result = CodingAgentService.evaluate(task.title, language, code, all_tests)

    submission = CodeSubmission.objects.create(
        task=task,
        user=request.user,
        language=language or "java",
        code=code,
        score=eval_result["score"],
        correctness_score=eval_result["correctness"],
        efficiency_score=eval_result["efficiency"],
        quality_score=eval_result["quality"],
        passed_tests=eval_result["passed"],
        total_tests=eval_result["total"],
        time_complexity=eval_result["timeComplexity"],
        space_complexity=eval_result["spaceComplexity"],
        feedback=eval_result["feedback"]
    )

    # Coding submission completes assignment
    assignment.status = 'COMPLETED'
    assignment.submitted_at = timezone.now()
    assignment.proof_url = None
    assignment.save()

    return Response(serialize_submission(submission), status=status.HTTP_201_CREATED)


@api_view(['GET'])
@permission_classes([IsAuthenticated, IsUser])
def my_coding_submissions_view(request):
    submissions = CodeSubmission.objects.filter(user=request.user).select_related('task', 'user').order_by('-submitted_at')
    return Response([serialize_submission(s) for s in submissions])


@api_view(['GET'])
def coding_leaderboard_view(request):
    limit_param = request.query_params.get('limit', '20')
    try:
        limit = max(1, min(int(limit_param), 50))
    except ValueError:
        limit = 20

    submissions = CodeSubmission.objects.all().select_related('user', 'task')
    user_scores = {}  # user_id -> {'name': name, 'tasks': {task_id: max_score}}

    for s in submissions:
        uid = s.user_id
        if uid not in user_scores:
            user_scores[uid] = {
                'userId': uid,
                'userName': s.user.name if s.user else "User",
                'tasks': {}
            }
        prev_best = user_scores[uid]['tasks'].get(s.task_id, 0.0)
        if s.score > prev_best:
            user_scores[uid]['tasks'][s.task_id] = s.score

    leaderboard = []
    for uid, data in user_scores.items():
        task_scores = list(data['tasks'].values())
        total_score = round(sum(task_scores), 1)
        solved_count = sum(1 for sc in task_scores if sc > 0)
        avg_score = round(total_score / len(task_scores), 1) if task_scores else 0.0

        leaderboard.append({
            "userId": uid,
            "userName": data['userName'],
            "totalScore": total_score,
            "solved": solved_count,
            "averageScore": avg_score
        })

    leaderboard.sort(key=lambda x: x["totalScore"], reverse=True)
    for idx, entry in enumerate(leaderboard):
        entry["rank"] = idx + 1

    return Response(leaderboard[:limit])
