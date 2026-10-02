# 📋 TaskFlow — Task Assignment & Tracking Platform

A full-stack Task Management System built with **Spring Boot** (backend) and **React/Vite** (frontend).  
Supports Admin and User roles with JWT authentication, Google OAuth, email notifications, and Supabase PostgreSQL.

---

## 📁 Project Structure

```
taskflow/
├── taskflow-backend/     ← Spring Boot 3 REST API (Java 25)
└── taskflow-frontend/    ← React 18 + Vite frontend
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

---

## 🛠️ Prerequisites

- **Java 25** (already installed)
- **Node.js 18+**
- **Gradle** (project uses gradlew — no install needed)
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

`
Host:     db.<your-project-ref>.supabase.co
Port:     5432
Database: postgres
Username: postgres
Password: <your-database-password>
`

---

## ⚙️ Backend Setup

`powershell
cd taskflow\taskflow-backend
`

### Set Environment Variables (PowerShell)

`powershell
     = "db.<your-project-ref>.supabase.co"
     = "5432"
     = "postgres"
     = "postgres"
 = "<your-supabase-db-password>"
  = "407f3498b3c292160d5b7a0f612803b9b4a1b023e32906b3a0e5b72183c27183"

# Optional — email notifications
 = "smtp.gmail.com"
 = "587"
 = "your-gmail@gmail.com"
 = "your-gmail-app-password"
`

### Start the Backend

`powershell
.\gradlew.bat bootRun
`

Runs on **http://localhost:8080**

> NOTE: If email is not configured, assignment removal notifications are printed to console instead.

---

## 🎨 Frontend Setup

`powershell
cd taskflow\taskflow-frontend
npm install
npm run dev
`

Runs on **http://localhost:5173**

> The frontend has a **smart fallback**: if the Spring Boot backend is offline, it uses a localStorage mock database automatically.

---

## 🔐 Default Login Credentials

Seeded by supabase_schema.sql:

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

### Tasks
| Method | URL | Role | Description |
|--------|-----|------|-------------|
| GET | /api/tasks | USER | Get published tasks |
| GET | /api/admin/tasks | ADMIN | Get all tasks |
| POST | /api/admin/tasks | ADMIN | Create task |
| PUT | /api/admin/tasks/{id} | ADMIN | Edit task |
| DELETE | /api/admin/tasks/{id} | ADMIN | Delete task |

### Assignments
| Method | URL | Role | Description |
|--------|-----|------|-------------|
| GET | /api/assignments/my | USER | My active assignments |
| GET | /api/assignments/my/all | USER | Full assignment history |
| POST | /api/assignments/assign?taskId={id} | USER | Self-assign |
| POST | /api/assignments/unassign/{id} | USER | Self-unassign |
| PUT | /api/assignments/update | USER | Update status/proof URL |
| GET | /api/admin/assignments | ADMIN | All assignments with filters |
| POST | /api/admin/assignments/{id}/remove | ADMIN | Remove + email notification |

---

## 🔑 Google OAuth Setup (Optional)

1. Go to Google Cloud Console → APIs & Services → Credentials
2. Create OAuth 2.0 Client ID (Web app)
3. Add http://localhost:5173 to Authorized JavaScript origins
4. Copy your Client ID into Login.jsx (replace the GOOGLE_CLIENT_ID placeholder)

---

## 🏗️ Production Build

### Backend JAR:
`powershell
.\gradlew.bat build
java -jar build\libs\taskflow-backend-0.0.1-SNAPSHOT.jar
`

### Frontend Static:
`powershell
npm run build   # output in dist/ — deploy to Netlify/Vercel
`

---

## 🗺️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite, React Router v6, Lucide Icons |
| Styling | Vanilla CSS, CSS variables, glassmorphism |
| Backend | Spring Boot 3.4, Spring Security, Spring Data JPA |
| Auth | JWT (JJWT 0.12.5), Google OAuth |
| Database | PostgreSQL via Supabase |
| Email | Spring Mail / JavaMailSender |
| Build | Gradle (backend), npm (frontend) |

---

## 🐛 Troubleshooting

- **Backend won't start?** → Check DB env vars, ensure Supabase project is active (not paused)
- **Frontend shows mock data?** → Start the backend first, then refresh
- **CORS errors?** → Backend allows ports 5173, 5174, 3000. Add yours in SecurityConfig.java
- **Email not sending?** → Use a Gmail App Password (not regular password). See: Google Account → Security → App Passwords

## Communication features added

This updated build includes:

- Persistent in-app notifications with a live SSE stream and unread/read state.
- Task discussion threads for users and administrators.
- Automatic deadline reminders using Spring Scheduler (due tomorrow and overdue), with in-app notifications and email attempts.

### New backend APIs

- `GET /api/notifications`
- `GET /api/notifications/stream` (SSE)
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`
- `GET /api/tasks/{taskId}/comments`
- `POST /api/tasks/{taskId}/comments`
- `DELETE /api/tasks/{taskId}/comments/{commentId}`

### Database

With `spring.jpa.hibernate.ddl-auto=update`, Hibernate creates the new `notifications` and `task_comments` tables automatically. For an explicit Supabase migration, run `taskflow-backend/new_features_migration.sql` once.

### Deadline schedule

Default reminder job: every day at 08:00 server local time. Override it with `TASKFLOW_REMINDER_CRON`.
