# 📋 TaskFlow — Task Assignment & Tracking Platform

A full-stack Task Management System built with **Python Django** (backend) and **React/Vite** (frontend).  
Supports Admin and User roles with JWT authentication, Google OAuth, email notifications, and Supabase PostgreSQL.

---

## 📁 Project Structure

```
taskflow/
├── taskflow-backend/    ← Python Django REST API
└── taskflow-frontend/   ← React 18 + Vite frontend
```

---

## ✅ Features

| Feature | Admin | User |
|---------|-------|------|
| Email & Password Auth | ✅ | ✅ |
| Google OAuth Login | ✅ | ✅ |
| Create / Edit / Delete Tasks | ✅ | ❌ |
| Publish / Draft Tasks | ✅ | ❌ |
| View All Assignments | ✅ | ❌ |
| Remove Any Assignment (+ Email) | ✅ | ❌ |
| View Published Tasks | ❌ | ✅ |
| Self-Assign a Task | ❌ | ✅ |
| Self-Unassign a Task | ❌ | ✅ |
| Update Status (In Progress / Completed) | ❌ | ✅ |
| Submit Proof URL (LinkedIn / GitHub) | ❌ | ✅ |
| View Full Assignment History | ❌ | ✅ |
| In-App Notifications (SSE) | ✅ | ✅ |
| Task Discussion Comments | ✅ | ✅ |
| Coding Lab (DSA Tasks + AI Evaluation) | ✅ | ✅ |

---

## 🛠️ Prerequisites

- **Python 3.10+**
- **Node.js 18+**
- **Supabase** project with PostgreSQL enabled

---

## 🗄️ Database Setup (Supabase)

### Step 1 — Create a Supabase Project
1. Go to https://supabase.com and sign in
2. Click **New Project** and set a strong database password
3. Wait for provisioning (~1 minute)

### Step 2 — Run the Schema SQL
1. In Supabase dashboard → **SQL Editor**
2. Paste contents of `taskflow-backend/supabase_schema.sql`
3. Click **Run** — creates tables + sample seed data

### Step 3 — Get Connection Details
In Supabase: **Project Settings → Database**

```
Host:     db.<your-project-ref>.supabase.co
Port:     5432
Database: postgres
Username: postgres
Password: <your-database-password>
```

---

## ⚙️ Backend Setup (Django)

### 1. Navigate to the Backend
```powershell
cd taskflow-backend
```

### 2. Install Dependencies
```powershell
python -m pip install -r requirements.txt
```

### 3. Configure Environment
Copy `.env.example` to `.env` and fill in your Supabase credentials:
```powershell
copy .env.example .env
# Then edit .env with your DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, EMAIL_PASSWORD, etc.
```

### 4. Verify Configuration
```powershell
python manage.py check
```

### 5. Start the Backend
```powershell
python manage.py runserver 8080
```
Or double-click `run-backend.bat`.

> **Runs on http://localhost:8080**

### Optional — Deadline Reminder Job
```powershell
python manage.py check_deadlines
```

---

## 🎨 Frontend Setup

```powershell
cd taskflow-frontend
npm install
npm run dev
```

Runs on **http://localhost:5173**

---

## 🔐 Default Login Credentials

Seeded by `supabase_schema.sql`:

| Role  | Email                 | Password    |
|-------|-----------------------|-------------|
| Admin | admin@example.com     | password123 |
| User  | keerthi@example.com   | password123 |
| User  | rahul@example.com     | password123 |
| User  | ananya@example.com    | password123 |
| User  | arjun@example.com     | password123 |

---

## 🌐 API Reference

### Auth (Public)
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/auth/login | Email + password login |
| POST | /api/auth/register | Register new user |
| POST | /api/auth/google | Google OAuth login/register |
| POST | /api/auth/forgot-password | Send reset email |
| POST | /api/auth/reset-password | Reset with token |

### Tasks
| Method | URL | Role | Description |
|--------|-----|------|-------------|
| GET | /api/tasks | USER | Get published tasks |
| GET | /api/tasks/{id} | USER | Get task detail |
| GET | /api/admin/tasks | ADMIN | Get all tasks |
| POST | /api/admin/tasks | ADMIN | Create task |
| PUT | /api/admin/tasks/{id} | ADMIN | Edit task |
| DELETE | /api/admin/tasks/{id} | ADMIN | Delete task |
| POST | /api/tasks/{id}/publish | ADMIN | Publish/unpublish task |

### Assignments
| Method | URL | Role | Description |
|--------|-----|------|-------------|
| GET | /api/assignments/my | USER | My active assignments |
| GET | /api/assignments/my/all | USER | Full assignment history |
| POST | /api/tasks/{id}/assign | USER | Self-assign |
| DELETE | /api/tasks/{id}/assignment | USER | Self-unassign |
| PATCH | /api/assignments/{id}/status | USER | Update status/proof URL |
| GET | /api/admin/assignments | ADMIN | All assignments |
| DELETE | /api/admin/assignments/{id} | ADMIN | Remove + email notification |

### Notifications
| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/notifications | Get my notifications |
| GET | /api/notifications/stream | SSE live stream |
| PATCH | /api/notifications/{id}/read | Mark one as read |
| PATCH | /api/notifications/read-all | Mark all as read |

### Task Comments
| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/tasks/{id}/comments | Get comments |
| POST | /api/tasks/{id}/comments | Post comment |
| DELETE | /api/tasks/{id}/comments/{cid} | Delete comment |

### Coding Lab
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/coding/generate | AI-generate coding task |
| POST | /api/coding/tasks | Save coding task |
| GET | /api/coding/tasks/{id}/tests | Get visible test cases |
| POST | /api/coding/tasks/{id}/run | Run code against tests |
| POST | /api/coding/tasks/{id}/submit | Submit + AI evaluation |
| GET | /api/coding/submissions/me | My submissions |
| GET | /api/coding/leaderboard | Leaderboard |

---

## 🔑 Google OAuth Setup (Optional)

1. Go to Google Cloud Console → APIs & Services → Credentials
2. Create OAuth 2.0 Client ID (Web app)
3. Add `http://localhost:5173` to Authorized JavaScript origins
4. Copy your Client ID into `Login.jsx` (replace the `GOOGLE_CLIENT_ID` placeholder)

---

## 🏗️ Production Deployment

### Backend (Gunicorn):
```bash
pip install gunicorn
gunicorn taskflow_project.wsgi:application --bind 0.0.0.0:8080
```

### Frontend Static:
```powershell
npm run build   # output in dist/ — deploy to Netlify/Vercel
```

---

## 🗺️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite, React Router v6, Lucide Icons |
| Styling | Vanilla CSS, CSS variables, glassmorphism |
| Backend | Python 3.12, Django 5, Django REST Framework |
| Auth | JWT (PyJWT), BCrypt, Google OAuth |
| Database | PostgreSQL via Supabase |
| Email | Django SMTP / Gmail App Password |
| AI | OpenRouter API (code evaluation & generation) |
| Code Runner | Local Java/Python subprocess execution |

---

## 🐛 Troubleshooting

- **Backend won't start?** → Run `python manage.py check`, verify `.env` has correct DB credentials
- **Supabase connection refused?** → Ensure your Supabase project is active (not paused on free tier)
- **CORS errors?** → Backend allows all origins in dev (`CORS_ALLOW_ALL_ORIGINS = True`)
- **Email not sending?** → Use a Gmail App Password (not regular password). Google Account → Security → App Passwords
- **JWT errors?** → Ensure `JWT_SECRET` in `.env` matches what was used to sign existing tokens
