# Employee Performance Evaluation System - Review 1 Project Plan
**Deadline: October 10, 2026 | Stack: Core Java (Java SE) + Swing GUI + JDBC**

---

## 1. Core Theme & Requirement Analysis (from Images)

**Project Title (as per GUVI doc): 18. Employee Performance Evaluation System**

**Summary:** Platform for Managers to evaluate employee performance, set goals, and provide feedback, while Employees track progress, view feedback, and set personal goals. System generates performance reports/analytics.

**User Roles:**
1.  **Manager:** Performance Evaluation, Goal Setting, Feedback Provision, Performance Reports (for team analysis), Team Management. Dashboard: tables + graphical reports + team panel.
2.  **Employee:** Performance Tracking (progress chart), View Feedback, Set Personal Goals, Performance Reports (self-assessment). Dashboard: chart, feedback history, goals interface, graphical reports.

**Review 1 Scope (from hackathon.guvi.in):**
> Define project structure, database design, database connectivity. Design UI, aesthetics, and responsiveness. Assessed on project planning, database integration, and UI/UX design principles.

**Deliverables for Review 1:**
- Presentation (PDF/PPT) with visuals, diagrams, screenshots
- Public GitHub Link with clear README (setup, requirements, run instructions)
- Code Quality: well-commented, modular, follows coding standards
- Working prototype (not full feature-complete, but structure + DB + UI skeleton must be ready)

**Marking Rubric Mapping (Java GUI Based - 33 Marks Total):**
1. OOP (Polymorphism, Inheritance, Exception Handling, Interfaces) - 10m
2. Collections & Generics - 6m
3. Multithreading & Synchronization - 4m
4. Classes for DB Operations - 7m
5. DB Connectivity (JDBC) - 3m
6. JDBC Implementation - 3m
=> Plan is optimized to demonstrate all 6 areas even in Review 1 skeleton.

---

## 2. Proposed Project Title

**Performetrics - Employee Performance Evaluation & Tracking System (Java Swing Desktop Application)**

Tagline: *Evaluate. Empower. Evolve.*

Alternative title for submission form: `Employee Performance Evaluation System - Core Java`

---

## 3. Technology Stack for Review 1 (Core Java Focus)

| Layer | Technology | Reason for Review 1 |
|-------|------------|---------------------|
| Language | Java 17 (LTS) | Core Java requirement |
| UI | Java Swing + FlatLaf Look & Feel | Demonstrates GUI, aesthetics, responsiveness without needing web server. Meets "GUI Based Projects" rubric. |
| Database | MySQL 8.0 (Primary) / SQLite (Fallback for demo) | Required for JDBC marks. MySQL shows real DB design; SQLite ensures offline demo. |
| Connectivity | JDBC (mysql-connector-j) | Explicit rubric item |
| Build | VS Code Java Project (no Maven/Gradle to keep simple) + `lib/` for JAR | Matches current workspace structure |
| Charts | JFreeChart 1.5.x | For Performance Reports graphical representation (Manager + Employee dashboards) |
| File I/O | CSV Export + Logging | Demonstrates File I/O + Exception Handling |

> If team prefers Web route: Replace Swing with Servlets + JSP (Tomcat 10). But for Review 1 (2 days), Swing is faster to achieve visual UI + DB connectivity. Choose ONE and stick to it.

---

## 4. Core Java Concepts Coverage Plan

| Concept | Where Implemented | Review 1 Evidence |
|---------|-------------------|-------------------|
| **OOP - Inheritance** | `User` (abstract) -> `Manager`, `Employee` ; `BaseEntity` -> `Evaluation`, `Goal`, `Feedback` | Class diagram in PPT + code |
| **OOP - Polymorphism** | `Evaluatable` interface with `calculateScore()`, `generateReport()` overridden; Method overloading in `GoalService` | Code + explanation slide |
| **OOP - Encapsulation** | All models with private fields + getters/setters + validation | Code |
| **OOP - Abstraction / Interfaces** | `DAO<T>` generic interface, `Reportable`, `AuthService` interface | Interface files |
| **Exception Handling** | Custom `InvalidRatingException`, `GoalDeadlineException`, `DBConnectionException`; try-with-resources in all DAO | Code + 1 slide |
| **Collections & Generics** | `List<Employee>`, `Map<Integer, List<Evaluation>>`, `DAO<T>` generic, `ArrayList`, `HashMap` for caching | Code + rubric mapping |
| **File I/O** | `FileLogger`, `ReportExporter.exportToCSV()`, `DBConfig.properties` loading | `util/` package |
| **Multithreading & Sync** | `SwingWorker` for DB loading (non-blocking UI), `synchronized` in `DatabaseConnection.getInstance()` | Required for 4 marks - show progress bar loading |
| **JDBC + DAO Classes** | `DatabaseConnection` (Singleton), `EmployeeDAO`, `EvaluationDAO`, `GoalDAO`, `FeedbackDAO` | 7+3+3 marks |

---

## 5. Modular Breakdown & Folder Structure

Target structure to create by Oct 10:

```
src/
├── App.java (Entry + Login Router)
├── model/
│   ├── User.java (abstract)
│   ├── Manager.java
│   ├── Employee.java
│   ├── Evaluation.java
│   ├── Goal.java
│   ├── Feedback.java
│   └── Team.java
├── dao/
│   ├── DAO.java (Generic Interface)
│   ├── DatabaseConnection.java (Singleton + synchronized)
│   ├── EmployeeDAO.java
│   ├── EvaluationDAO.java
│   ├── GoalDAO.java
│   └── FeedbackDAO.java
├── service/
│   ├── AuthService.java
│   ├── EvaluationService.java
│   └── ReportService.java
├── ui/
│   ├── LoginFrame.java
│   ├── ManagerDashboard.java (TabbedPane: Evaluation/Goals/Feedback/Reports/Team)
│   ├── EmployeeDashboard.java (TabbedPane: Tracking/Feedback/Goals/Reports)
│   └── components/
│       ├── PerformanceChartPanel.java (JFreeChart wrapper)
│       └── RoundedPanel.java (Aesthetics)
├── util/
│   ├── DBConfig.java (loads db.properties)
│   ├── FileLogger.java
│   ├── ReportExporter.java (CSV via File I/O)
│   └── Validator.java
└── exception/
    ├── InvalidRatingException.java
    └── GoalDeadlineException.java

lib/
├── mysql-connector-j-8.3.0.jar
├── jfreechart-1.5.4.jar
└── flatlaf-3.4.1.jar

db/
├── schema.sql
└── seed_data.sql

docs/
├── ER_Diagram.png
└── screenshots/

README.md
db.properties
```

**Feature Modules & Ownership:**

**Module A: Authentication & User Management**
- LoginFrame with role-based routing (Manager/Employee)
- `User` hierarchy + `AuthService`

**Module B: Manager - Performance Evaluation**
- Table (JTable) listing evaluations: employee, criteria, score, date
- Add/Edit Evaluation dialog: criteria (Communication, Technical, Punctuality, Teamwork - 1-5 rating), auto-calc weighted score
- DAO: `evaluations` table

**Module C: Manager - Goal Setting & Feedback**
- Goal Setting: form (description, targetDate, assignedEmployee) -> JTable confirmation
- Feedback Provision: form (comments, rating, evaluationId) -> JTable
- Reuse for Employee Personal Goals (same `goals` table with `is_personal` flag)

**Module D: Employee - Tracking & Personal Goals**
- Performance Tracking: JFreeChart line chart (score vs time) from `evaluations`
- Personal Goals: CRUD interface + progress status (Pending/In Progress/Completed)

**Module E: Performance Reports (Both Roles)**
- Graphical: JFreeChart bar/pie for Manager (team avg), line for Employee
- Detailed report: JTable + Export to CSV (File I/O demo)
- Multithreaded loading via SwingWorker

**Module F: Team Management (Manager Only)**
- Panel for adding members, assigning roles (Lead, Developer, QA)
- `teams` + `team_members` tables

---

## 6. Database Design (For Review 1 ER Diagram Slide)

**Tables (schema.sql):**

```sql
users (user_id PK, name, email UNIQUE, password, role ENUM('MANAGER','EMPLOYEE'), created_at)
employees (employee_id PK FK->users, department, designation, manager_id FK->users)
evaluations (eval_id PK, employee_id FK, manager_id FK, criteria_json, overall_score, comments, eval_date)
goals (goal_id PK, title, description, assigned_to FK, assigned_by FK, target_date, status ENUM, is_personal BOOLEAN, created_at)
feedback (feedback_id PK, employee_id FK, manager_id FK, eval_id FK nullable, comments TEXT, rating INT 1-5, given_on)
teams (team_id PK, team_name, manager_id FK, created_at)
team_members (team_id FK, employee_id FK, role VARCHAR(50), PRIMARY KEY(team_id, employee_id))
```

**ER Relations:** Manager 1:N Employee, Employee 1:N Evaluations, Employee 1:N Goals, Employee 1:N Feedback, Team M:N Employee

**For Review 1:** Show ER diagram (draw.io), screenshot of MySQL Workbench, and `DatabaseConnection.java` test connection success log.

---

## 7. Development Timeline: Oct 8 - Oct 10 (48-Hour Sprint)

### Day 0: TODAY - Oct 8 (Evening - 4 Hours)
**Goal: Project Initialization + DB Design (Review 1 - 50%)**
- [ ] 18:00-19:00 - Finalize this plan, create GitHub repo (public), push initial structure + README
- [ ] 19:00-20:30 - Create `model/` classes (User, Manager, Employee, Evaluation, Goal, Feedback) + `exception/` + `DAO` interface. Demonstrates OOP + Generics.
- [ ] 20:30-22:00 - Design `schema.sql` + create DB in MySQL + test `DatabaseConnection` singleton (synchronized). Capture screenshots for PPT.
- Deliverable: GitHub commit "Initial project structure & DB connectivity"

### Day 1: Oct 9 (Full Day - 8 Hours)
**Goal: DAO + UI Skeleton (Review 1 - 80%)**
- [ ] 09:00-11:30 - Implement `dao/` classes (EmployeeDAO, EvaluationDAO) with JDBC + try-with-resources + Collections (List<Evaluation>). Test CRUD via console.
- [ ] 11:30-13:30 - Build `ui/LoginFrame.java` + `ManagerDashboard.java` (JTabbedPane with 5 tabs - empty tables). Apply FlatLaf for aesthetics.
- [ ] 14:30-16:30 - Build `EmployeeDashboard.java` (4 tabs) + `PerformanceChartPanel.java` (JFreeChart dummy data). Implement SwingWorker loading simulation.
- [ ] 16:30-18:30 - Implement `util/FileLogger`, `ReportExporter`, `Validator` (File I/O + Exception Handling). Add CSV export button.
- [ ] Evening - Start PPT: Slide 1-6 (Title, Problem, Solution, Tech Stack, Architecture, ER Diagram)
- Deliverable: Runnable GUI that switches login -> dashboards with dummy data + DB connected

### Day 2: Oct 10 (Review Day - 4 Hours before submission)
**Goal: Polish + Documentation + Submission**
- [ ] 09:00-10:30 - Wire 1 end-to-end flow: Manager adds Evaluation -> DB -> Employee sees it in Feedback History + Chart updates. This is sufficient for demo, not all features.
- [ ] 10:30-11:30 - Finish PPT (UI Screenshots, Code Quality - OOP diagram, Collections usage, JDBC demo, Future Work). Export PDF.
- [ ] 11:30-12:30 - Update README.md (Project description, Features, Tech stack, Setup steps: clone, add lib JARs, run schema.sql, configure db.properties, run App.java, screenshots, team). Ensure GitHub repo is public and reviewers have access.
- [ ] 12:30-13:00 - Final test run, record 30-sec screen capture, verify presentation + GitHub link, submit via hackathon.guvi.in form before deadline. Review submission requirements checklist.
- Deliverable: Submission ready

**Buffer:** If MySQL setup fails, fall back to SQLite (`sqlite-jdbc`) or file-based storage (Collections + File I/O) to still show functionality.

---

## 8. Review 1 Presentation Outline (10 Slides Recommended)

1. Title: Performetrics - Team, Problem Statement 18
2. Problem Understanding & Solution Design (8 marks rubric)
3. System Architecture (Modular diagram - Model-DAO-Service-UI)
4. Database Design (ER Diagram + schema.sql screenshot)
5. UI/UX Design (Figma/wireframe -> Swing screenshots - Login, Manager Dashboard, Employee Dashboard)
6. Core Java Concepts Mapping (Table from Section 4 + code snippet)
7. Database Connectivity Demo (DatabaseConnection code + MySQL Workbench screenshot + log)
8. Key Feature Implemented (End-to-end Evaluation flow demo)
9. Code Quality (Modular structure, Exception handling, Comments, GitHub link)
10. Future Work (Remaining features for Final Review + Timeline) + Thank You

---

## 9. Immediate Next Actions for You

1. Choose DB: MySQL (recommended for full marks) or SQLite (easier setup). Install `mysql-connector-j` to `lib/`.
2. Create GitHub repo `EmployeePerformanceSystem`, push current workspace, make public.
3. Run: create `model/User.java` first to lock OOP hierarchy.
4. Use this timeline strictly - Review 1 does NOT require 100% features, only structure + DB + UI. Avoid feature creep.

---

## 10. Risk Mitigation

- **Time Shortage:** Prioritize Module A + B + E (Login + Evaluation + Chart). Other tabs can show "Coming Soon" tables with dummy data - still counts as UI design.
- **JDBC Issues:** Keep `DatabaseConnection` with fallback: if MySQL fails, log to file and use in-memory `ArrayList` (Collections) so demo doesn't break.
- **UI Not Responsive:** Use `GridBagLayout`/`BorderLayout` + `setPreferredSize`, test at 1366x768. FlatLaf ensures modern aesthetics with 1 line.

This plan ensures all rubric items are visible by Oct 10 even with partial implementation, maximizing Review 1 score.
