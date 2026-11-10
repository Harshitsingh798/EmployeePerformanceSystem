-- Migration v2: Hash passwords for existing users
-- Run after schema.sql + seed_data.sql
USE employee_performance_db;

UPDATE users SET password = '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8' WHERE email = 'manager@company.com' AND password = 'manager123';
UPDATE users SET password = '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8' WHERE email = 'priya@company.com' AND password = 'emp123';
UPDATE users SET password = '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8' WHERE email = 'amit@company.com' AND password = 'emp123';
UPDATE users SET password = '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8' WHERE email = 'sneha@company.com' AND password = 'emp123';

-- Verify
SELECT user_id, name, email, password, role FROM users;
