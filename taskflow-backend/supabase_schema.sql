-- ==============================================================================
-- TaskFlow Database Schema (Supabase / PostgreSQL)
-- ==============================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    google_id VARCHAR(255),
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tasks Table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    instructions TEXT,
    deadline DATE NOT NULL,
    proof_requirement VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Assignments Table
CREATE TABLE IF NOT EXISTS assignments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED_NOT_STARTED' CHECK (status IN ('ASSIGNED_NOT_STARTED', 'STARTED_NOT_COMPLETED', 'COMPLETED', 'REMOVED')),
    proof_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    removed_at TIMESTAMP,
    removed_reason TEXT
);

-- 4. Indexes for Performance & Integrity
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_assignments_user_id ON assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_assignments_task_id ON assignments(task_id);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON assignments(status);
CREATE INDEX IF NOT EXISTS idx_assignments_active ON assignments(is_active);

-- 5. Prevent duplicate active assignments for the same User and Task
-- Remove the legacy three-column UNIQUE constraint if an older database has it.
ALTER TABLE assignments DROP CONSTRAINT IF EXISTS unique_active_user_task;

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_user_task 
ON assignments (user_id, task_id) 
WHERE is_active = TRUE;

-- ==============================================================================
-- Seed Data (Default Admin & Users - BCrypt password for 'password123')
-- ==============================================================================

-- Admin User
INSERT INTO users (name, email, password_hash, role)
VALUES ('Admin User', 'admin@example.com', '$2a$10$dXJ3ADWyyTXpSnT2195SDeoR24ACZODgB.Cg9zZ8B0aC1pX9p8K/q', 'ADMIN')
ON CONFLICT (email) DO NOTHING;

-- Regular Users
INSERT INTO users (name, email, password_hash, role)
VALUES 
('Keerthi', 'keerthi@example.com', '$2a$10$dXJ3ADWyyTXpSnT2195SDeoR24ACZODgB.Cg9zZ8B0aC1pX9p8K/q', 'USER'),
('Rahul Sharma', 'rahul@example.com', '$2a$10$dXJ3ADWyyTXpSnT2195SDeoR24ACZODgB.Cg9zZ8B0aC1pX9p8K/q', 'USER'),
('Ananya Rao', 'ananya@example.com', '$2a$10$dXJ3ADWyyTXpSnT2195SDeoR24ACZODgB.Cg9zZ8B0aC1pX9p8K/q', 'USER'),
('Arjun Kumar', 'arjun@example.com', '$2a$10$dXJ3ADWyyTXpSnT2195SDeoR24ACZODgB.Cg9zZ8B0aC1pX9p8K/q', 'USER')
ON CONFLICT (email) DO NOTHING;

-- Sample Published Tasks
INSERT INTO tasks (id, title, description, instructions, deadline, proof_requirement, status, created_by)
VALUES 
(1, 'Create LinkedIn AI Post', 'Create an engaging LinkedIn post explaining a recent AI concept.', 'Research the topic, prepare a professional post and publish it on LinkedIn.', '2026-08-20', 'LinkedIn post URL', 'PUBLISHED', 1),
(2, 'Build Spring Boot REST API', 'Create a REST API using Spring Boot with proper CRUD operations.', 'Create endpoints, implement validation and test the APIs using Postman.', '2026-08-25', 'GitHub repository URL', 'PUBLISHED', 1),
(3, 'AI Research Summary', 'Research a recent AI development and create a concise summary.', 'Choose an AI topic and prepare a well-structured research summary.', '2026-08-28', 'Document or publication URL', 'PUBLISHED', 1),
(4, 'React Dashboard UI', 'Design a modern responsive dashboard using React.', 'Build the dashboard with reusable components and responsive layouts.', '2026-09-01', 'GitHub repository URL', 'PUBLISHED', 1)
ON CONFLICT (id) DO NOTHING;

-- Reset tasks sequence if needed
SELECT setval('tasks_id_seq', (SELECT MAX(id) FROM tasks));

-- 6. Task Comments / Discussion
CREATE TABLE IF NOT EXISTS task_comments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_comments_task_created ON task_comments(task_id, created_at);
CREATE INDEX IF NOT EXISTS idx_task_comments_user ON task_comments(user_id);

-- 7. Persistent in-app notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_task_type ON notifications(task_id, type);
