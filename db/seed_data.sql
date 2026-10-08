-- Seed data for testing
USE employee_performance_db;

-- Sample Manager
INSERT INTO users (name, email, password, role, department) VALUES
('Rajesh Kumar', 'manager@company.com', 'manager123', 'MANAGER', 'Engineering');
INSERT INTO users (name, email, password, role, department, designation, manager_id) VALUES
('Priya Sharma', 'priya@company.com', 'emp123', 'EMPLOYEE', 'Engineering', 'Developer', 1),
('Amit Patel', 'amit@company.com', 'emp123', 'EMPLOYEE', 'Engineering', 'QA Engineer', 1),
('Sneha Gupta', 'sneha@company.com', 'emp123', 'EMPLOYEE', 'Marketing', 'Designer', 1);

-- Sample Evaluations
INSERT INTO evaluations (employee_id, manager_id, communication, technical, punctuality, teamwork, overall_score, comments) VALUES
(2, 1, 4, 5, 4, 5, 4.5, 'Excellent technical skills, needs slight improvement in communication'),
(3, 1, 5, 3, 5, 4, 4.25, 'Great team player, focus on technical depth'),
(4, 1, 5, 4, 4, 5, 4.5, 'Creative and consistent performer');

-- Sample Goals
INSERT INTO goals (title, description, assigned_to, assigned_by, target_date, status, is_personal) VALUES
('Complete Project Alpha', 'Deliver MVP by deadline', 2, 1, '2026-11-15', 'In Progress', FALSE),
('Learn Spring Boot', 'Complete online course', 2, 2, '2026-12-01', 'Pending', TRUE),
('Improve Test Coverage', 'Achieve 80% coverage', 3, 1, '2026-10-30', 'Pending', FALSE);

-- Sample Feedback
INSERT INTO feedback (employee_id, manager_id, comments, rating) VALUES
(2, 1, 'Outstanding work on the recent sprint!', 5),
(3, 1, 'Good progress, keep focusing on automation', 4);

-- Sample Team
INSERT INTO teams (team_name, manager_id) VALUES ('Alpha Team', 1);
INSERT INTO team_members (team_id, employee_id, role) VALUES (1, 2, 'Lead Developer'), (1, 3, 'QA'), (1, 4, 'Designer');
