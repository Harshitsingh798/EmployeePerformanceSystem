-- Seed data for testing
-- Run this AFTER schema.sql and migration_v2.sql
--
-- Demo credentials (documented in README.md):
--   manager@company.com / manager123   (MANAGER)
--   priya@company.com  / emp123        (EMPLOYEE)
--   amit@company.com   / emp123        (EMPLOYEE)
--   sneha@company.com  / emp123        (EMPLOYEE)
--
-- The password column holds a PBKDF2-HMAC-SHA256 hash. To generate a hash for
-- a different password run:  java -cp ".;lib/*" util.PasswordHashTool "MyPassword"
USE employee_performance_db;

-- Sample Manager
INSERT INTO users (name, email, password, role, department) VALUES
('Rajesh Kumar', 'manager@company.com', 'pbkdf2$210000$B6uXjdwYvzSGacw8bihxig==$SVqlNsjydYvMtJMAVeqRsAqsRCqNIP1tvLDmQllzYeA=', 'MANAGER', 'Engineering');
INSERT INTO users (name, email, password, role, department, designation, manager_id) VALUES
('Priya Sharma', 'priya@company.com', 'pbkdf2$210000$ofm3xx+yUV1A9+C6M2kl/A==$ae+ARbWzlRwE5/RgOxTbclBi4Nw2wXvlbD7dOnKSLiA=', 'EMPLOYEE', 'Engineering', 'Developer', 1),
('Amit Patel', 'amit@company.com', 'pbkdf2$210000$ofm3xx+yUV1A9+C6M2kl/A==$ae+ARbWzlRwE5/RgOxTbclBi4Nw2wXvlbD7dOnKSLiA=', 'EMPLOYEE', 'Engineering', 'QA Engineer', 1),
('Sneha Gupta', 'sneha@company.com', 'pbkdf2$210000$ofm3xx+yUV1A9+C6M2kl/A==$ae+ARbWzlRwE5/RgOxTbclBi4Nw2wXvlbD7dOnKSLiA=', 'EMPLOYEE', 'Marketing', 'Designer', 1);

-- Sample Evaluations
INSERT INTO evaluations (employee_id, manager_id, communication, technical, punctuality, teamwork, overall_score, comments) VALUES
(2, 1, 4, 5, 4, 5, 4.50, 'Excellent technical skills, needs slight improvement in communication'),
(3, 1, 5, 3, 5, 4, 4.25, 'Great team player, focus on technical depth'),
(4, 1, 5, 4, 4, 5, 4.50, 'Creative and consistent performer');

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
