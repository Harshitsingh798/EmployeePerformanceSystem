-- Employee Performance Evaluation System - Database Schema
-- Run this in MySQL Workbench or command line: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS employee_performance_db;
USE employee_performance_db;

-- Users table (both Manager & Employee)
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role ENUM('MANAGER','EMPLOYEE') NOT NULL,
    department VARCHAR(100),
    designation VARCHAR(100),
    manager_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (manager_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- Evaluations table
CREATE TABLE IF NOT EXISTS evaluations (
    eval_id INT PRIMARY KEY AUTO_INCREMENT,
    employee_id INT NOT NULL,
    manager_id INT NOT NULL,
    communication INT CHECK (communication BETWEEN 1 AND 5),
    technical INT CHECK (technical BETWEEN 1 AND 5),
    punctuality INT CHECK (punctuality BETWEEN 1 AND 5),
    teamwork INT CHECK (teamwork BETWEEN 1 AND 5),
    overall_score DOUBLE,
    comments TEXT,
    eval_date DATE DEFAULT (CURRENT_DATE),
    FOREIGN KEY (employee_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Goals table (Manager-assigned + Personal)
CREATE TABLE IF NOT EXISTS goals (
    goal_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    assigned_to INT NOT NULL,
    assigned_by INT NOT NULL,
    target_date DATE NOT NULL,
    status ENUM('Pending','In Progress','Completed') DEFAULT 'Pending',
    is_personal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assigned_to) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Feedback table
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id INT PRIMARY KEY AUTO_INCREMENT,
    employee_id INT NOT NULL,
    manager_id INT NOT NULL,
    eval_id INT,
    comments TEXT NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    given_on DATE DEFAULT (CURRENT_DATE),
    FOREIGN KEY (employee_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (eval_id) REFERENCES evaluations(eval_id) ON DELETE SET NULL
);

-- Teams table
CREATE TABLE IF NOT EXISTS teams (
    team_id INT PRIMARY KEY AUTO_INCREMENT,
    team_name VARCHAR(100) NOT NULL,
    manager_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (manager_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Team members (M:N)
CREATE TABLE IF NOT EXISTS team_members (
    team_id INT NOT NULL,
    employee_id INT NOT NULL,
    role VARCHAR(50),
    PRIMARY KEY (team_id, employee_id),
    FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES users(user_id) ON DELETE CASCADE
);
