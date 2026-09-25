from django.urls import path
from . import views

urlpatterns = [
    # 1. Auth
    path('auth/login', views.login_view, name='auth_login'),
    path('auth/register', views.register_view, name='auth_register'),
    path('auth/google', views.google_login_view, name='auth_google'),
    path('auth/me', views.auth_me_view, name='auth_me'),
    path('auth/forgot-password', views.forgot_password_view, name='auth_forgot_password'),
    path('auth/reset-password', views.reset_password_view, name='auth_reset_password'),

    # 2. Users
    path('users/me', views.user_me_view, name='user_me'),
    path('users/me/tasks', views.my_active_assignments_view, name='user_me_tasks'),
    path('users/me/history', views.my_all_assignments_view, name='user_me_history'),
    path('users/<int:user_id>', views.user_by_id_view, name='user_by_id'),
    path('users', views.admin_all_users_view, name='users_list'),

    # 3. Tasks
    path('tasks', views.get_published_tasks_view, name='tasks_published'),
    path('tasks/<int:task_id>', views.get_task_by_id_view, name='task_by_id'),
    path('tasks/<int:task_id>/assign', views.assign_task_view, name='task_assign'),
    path('tasks/<int:task_id>/assignment', views.unassign_by_task_id_view, name='task_unassign'),
    path('tasks/<int:task_id>/publish', views.publish_task_view, name='task_publish'),
    path('tasks/<int:task_id>/comments', views.task_comments_view, name='task_comments'),
    path('tasks/<int:task_id>/comments/<int:comment_id>', views.delete_task_comment_view, name='task_comment_delete'),

    # 4. Assignments
    path('assignments/my', views.my_active_assignments_view, name='assignments_my_active'),
    path('assignments/my/all', views.my_all_assignments_view, name='assignments_my_all'),
    path('assignments/assign', views.assign_task_view, name='assignments_assign_compat'),
    path('assignments/<int:assignment_id>', views.get_assignment_by_id_view, name='assignment_by_id'),
    path('assignments/<int:assignment_id>/status', views.update_assignment_status_view, name='assignment_status'),
    path('assignments/<int:assignment_id>/proof', views.get_assignment_proof_view, name='assignment_proof'),
    path('assignments/unassign/<int:assignment_id>', views.unassign_by_id_view, name='assignment_unassign_compat'),
    path('assignments/update', views.update_assignment_status_view, name='assignment_update_compat'),

    # 5. Admin
    path('admin/dashboard', views.admin_dashboard_view, name='admin_dashboard'),
    path('admin/tasks', views.admin_all_tasks_view, name='admin_tasks'),
    path('admin/tasks/<int:task_id>', views.update_task_view, name='admin_task_update'),
    path('admin/tasks/<int:task_id>/publish', views.publish_task_view, name='admin_task_publish'),
    path('admin/tasks/<int:task_id>/assignments', views.admin_task_assignments_view, name='admin_task_assignments'),
    path('admin/assignments', views.admin_assignments_view, name='admin_assignments'),
    path('admin/assignments/<int:assignment_id>', views.admin_remove_assignment_view, name='admin_assignment_remove'),
    path('admin/assignments/<int:assignment_id>/remove', views.admin_remove_assignment_view, name='admin_assignment_remove_compat'),
    path('admin/users', views.admin_all_users_view, name='admin_users'),
    path('admin/users/<int:user_id>/assignments', views.admin_user_assignments_view, name='admin_user_assignments'),

    # 6. Notifications
    path('notifications', views.my_notifications_view, name='notifications_mine'),
    path('notifications/unread-count', views.unread_notifications_count_view, name='notifications_unread_count'),
    path('notifications/<int:notification_id>/read', views.mark_notification_read_view, name='notification_mark_read'),
    path('notifications/read-all', views.mark_all_notifications_read_view, name='notifications_mark_all_read'),
    path('notifications/stream', views.notification_stream_view, name='notifications_stream'),

    # 7. Coding Lab
    path('coding/generate', views.coding_generate_view, name='coding_generate'),
    path('coding/tasks', views.coding_save_task_view, name='coding_save_task'),
    path('coding/tasks/<int:task_id>/tests', views.get_visible_tests_view, name='coding_visible_tests'),
    path('coding/tasks/<int:task_id>/run', views.coding_run_view, name='coding_run'),
    path('coding/tasks/<int:task_id>/submit', views.coding_submit_view, name='coding_submit'),
    path('coding/submissions/me', views.my_coding_submissions_view, name='coding_my_submissions'),
    path('coding/leaderboard', views.coding_leaderboard_view, name='coding_leaderboard'),
]
