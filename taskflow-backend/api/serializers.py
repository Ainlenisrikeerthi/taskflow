from rest_framework import serializers
from .models import (
    User, Task, Assignment, Notification, TaskComment,
    CodingTestCase, CodeSubmission
)


def format_iso(dt):
    if not dt:
        return None
    return dt.isoformat()


def serialize_user(user):
    if not user:
        return None
    return {
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "role": user.role,
        "createdAt": format_iso(user.created_at),
    }


def serialize_task(task, assigned_count=None, completed_count=None):
    if not task:
        return None
    
    if assigned_count is None:
        assigned_count = Assignment.objects.filter(task_id=task.id, is_active=True).count()
    if completed_count is None:
        completed_count = Assignment.objects.filter(task_id=task.id, status='COMPLETED', is_active=True).count()

    created_by_id = task.created_by_id
    created_by_name = None
    if task.created_by:
        created_by_name = task.created_by.name

    return {
        "id": task.id,
        "title": task.title,
        "description": task.description,
        "instructions": task.instructions,
        "deadline": task.deadline.isoformat() if task.deadline else None,
        "proofRequirement": task.proof_requirement,
        "status": task.status,
        "taskType": task.task_type or "GENERAL",
        "difficulty": task.difficulty,
        "starterCode": task.starter_code,
        "createdById": created_by_id,
        "createdByName": created_by_name,
        "assignedCount": assigned_count,
        "completedCount": completed_count,
        "createdAt": format_iso(task.created_at),
        "updatedAt": format_iso(task.updated_at),
    }


def serialize_assignment(assignment):
    if not assignment:
        return None

    task_data = serialize_task(assignment.task) if assignment.task else None
    user_data = serialize_user(assignment.user) if assignment.user else None

    return {
        "id": assignment.id,
        "taskId": assignment.task_id,
        "task": task_data,
        "userId": assignment.user_id,
        "user": user_data,
        "status": assignment.status,
        "proofUrl": assignment.proof_url,
        "isActive": assignment.is_active,
        "assignedAt": format_iso(assignment.assigned_at),
        "submittedAt": format_iso(assignment.submitted_at),
        "removedAt": format_iso(assignment.removed_at),
        "removedReason": assignment.removed_reason,
    }


def serialize_notification(n):
    if not n:
        return None
    return {
        "id": n.id,
        "type": n.type,
        "title": n.title,
        "message": n.message,
        "isRead": n.is_read,
        "createdAt": format_iso(n.created_at),
        "taskId": n.task_id if n.task_id else None,
    }


def serialize_comment(c):
    if not c:
        return None
    return {
        "id": c.id,
        "taskId": c.task_id,
        "userId": c.user_id,
        "userName": c.user.name if c.user else "User",
        "userRole": c.user.role if c.user else "USER",
        "message": c.message,
        "createdAt": format_iso(c.created_at),
        "updatedAt": format_iso(c.updated_at),
    }


def serialize_test_case(tc, include_hidden=True):
    if not tc:
        return None
    data = {
        "id": tc.id,
        "input": tc.input,
        "expectedOutput": tc.expected_output,
    }
    if include_hidden:
        data["hidden"] = tc.hidden
    return data


def serialize_submission(s):
    if not s:
        return None
    return {
        "id": s.id,
        "taskId": s.task_id,
        "taskTitle": s.task.title if s.task else "Task",
        "userName": s.user.name if s.user else "User",
        "language": s.language,
        "score": s.score,
        "correctnessScore": s.correctness_score,
        "efficiencyScore": s.efficiency_score,
        "qualityScore": s.quality_score,
        "passedTests": s.passed_tests,
        "totalTests": s.total_tests,
        "feedback": s.feedback,
        "timeComplexity": s.time_complexity,
        "spaceComplexity": s.space_complexity,
        "submittedAt": format_iso(s.submitted_at),
    }
