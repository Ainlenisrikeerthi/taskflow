import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'taskflow_project.settings')
django.setup()

from rest_framework.test import APIClient
from api.models import User, Task, Assignment
from api.security import generate_jwt

client = APIClient()

print("--- 1. Testing Public Tasks Endpoint ---")
response = client.get('/api/tasks')
print(f"Status: {response.status_code}")
tasks = response.json()
print(f"Published tasks returned: {len(tasks)}")
if tasks:
    print(f"Sample task: id={tasks[0].get('id')}, title={tasks[0].get('title')}, status={tasks[0].get('status')}")

print("\n--- 2. Testing Authenticated Admin Request ---")
admin_user = User.objects.filter(role='ADMIN').first()
if admin_user:
    token = generate_jwt(admin_user.email)
    client.credentials(HTTP_AUTHORIZATION=f'Bearer {token}')
    
    # Test dashboard
    dash_resp = client.get('/api/admin/dashboard')
    print(f"Admin Dashboard Status: {dash_resp.status_code}")
    print("Dashboard Data:", dash_resp.json())
    
    # Test admin tasks list
    admin_tasks = client.get('/api/admin/tasks')
    print(f"Admin Tasks Status: {admin_tasks.status_code}, count={len(admin_tasks.json())}")

    # Test admin users list
    admin_users = client.get('/api/admin/users')
    print(f"Admin Users Status: {admin_users.status_code}, count={len(admin_users.json())}")
else:
    print("No admin user found in DB")

print("\n--- 3. Testing Regular User Request ---")
reg_user = User.objects.filter(role='USER').first()
if reg_user:
    user_token = generate_jwt(reg_user.email)
    user_client = APIClient()
    user_client.credentials(HTTP_AUTHORIZATION=f'Bearer {user_token}')
    
    # User Profile
    me_resp = user_client.get('/api/users/me')
    print(f"User Me Status: {me_resp.status_code}")
    print("User Me Data:", me_resp.json())
    
    # My Active assignments
    my_tasks = user_client.get('/api/assignments/my')
    print(f"My Active Assignments Status: {my_tasks.status_code}, count={len(my_tasks.json())}")
    
    # Leaderboard
    lb = user_client.get('/api/coding/leaderboard')
    print(f"Leaderboard Status: {lb.status_code}, count={len(lb.json())}")

print("\n--- All initial API tests executed successfully! ---")
