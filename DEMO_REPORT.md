# Performetrics — Employee Performance Evaluation System

**Teacher Demo Report — Step-by-Step Project Walkthrough**

**Project:** Performetrics — Employee Performance Evaluation & Tracking System  
**Stack:** Core Java 17+ · Java Swing (FlatLaf) · MySQL 8.0 via JDBC · JFreeChart charts  
**Problem:** GUVI Hackathon Problem #18 — Employee Performance Evaluation System  
**Demo credentials:** see Section 6

---

## 1. What the project does (in one line)

Performetrics is a desktop application where **managers** evaluate employee performance, set goals, give feedback and generate team reports — while **employees** track their own progress, read feedback and manage personal goals. Everything is stored in a MySQL database and shown through a modern Swing GUI with live charts.

---

## 2. The two roles and what each can do

| Manager dashboard (5 tabs) | Employee dashboard (4 tabs) |
|---|---|
| **Performance Evaluation** — add, edit, delete evaluations rated on Communication, Technical, Punctuality and Teamwork (1–5). Auto-calculates the weighted overall score. | **Performance Tracking** — line chart of the employee's score over time, plus the full evaluation history and running average. |
| **Goal Setting** — assign goals to any team member with a target date and status (Pending → In Progress → Completed). | **Feedback History** — every feedback item received, with rating and date. |
| **Feedback Provision** — written feedback with a 1–5 rating, optionally linked to an evaluation. | **Personal Goals** — set, edit, complete and delete personal goals, with a status pie chart. |
| **Performance Reports** — bar chart of average score per employee and pie chart of team criteria averages; export to CSV. | **Performance Reports** — bar and pie charts of the employee's own criteria averages; export to CSV. |
| **Team Management** — add employees to teams, change a member's role, remove members. | — |

**Key security rule:** every database query is filtered by the signed-in user's id. A manager only sees their own team; an employee only sees their own goals, feedback and evaluations — even if they guess another user's id.

---

## 3. Step-by-step demo script (what to show the teacher)

Below is the exact order to run through, with what to click and what to say at each step.

### Step 0 — Start the application

1. Make sure MySQL 8.0 is running (`start-mysql.bat` if it is installed locally).
2. Open the project folder in VS Code (or any terminal).
3. Run the app:
   - **VS Code:** open `src/App.java` and run it (lib/ is already on the classpath in `.vscode/settings.json`).
   - **Terminal:**  
     ```bash
     javac -encoding UTF-8 -cp "lib/*" -d bin -sourcepath src src/App.java
     java -cp "bin;lib/*" App
     ```
   - **Double-click:** `run.bat` compiles and starts the app automatically.

**What to say:** *"This is the entry point. It loads the FlatLaf look-and-feel, checks that the MySQL JDBC driver is present, verifies the database connection, and opens the login window."*

---

### Step 1 — Login screen (role-based routing)

The `LoginFrame` opens. Show these things:

1. Type an e-mail and password.
2. Choose the role from the dropdown: **MANAGER** or **EMPLOYEE**.
3. Click **Login**. The app authenticates against the `users` table and opens the matching dashboard.
4. Try the wrong password or wrong role — it is rejected with an error message.

**Demo credentials:**

| Role | E-mail | Password |
|---|---|---|
| Manager | `manager@company.com` | `manager123` |
| Employee | `priya@company.com` | `emp123` |
| Employee | `amit@company.com` | `emp123` |
| Employee | `sneha@company.com` | `emp123` |

**What to say:** *"Authentication uses PBKDF2-HMAC-SHA256 with 210,000 iterations and a random salt per password — the OWASP-recommended scheme. The role is checked in SQL, so logging in as 'MANAGER' with an employee account fails. Passwords are never stored as plain text."*

---

### Step 2 — Manager dashboard: Performance Evaluation tab

Log in as the manager (`manager@company.com` / `manager123`).

1. The **Performance Evaluation** tab shows a table of all evaluations the manager has written: employee name, the four criteria, overall score, date and comments.
2. Click **Add Evaluation**.
3. Pick an employee from the dropdown (only the manager's own team members appear).
4. Choose ratings 1–5 for Communication, Technical, Punctuality and Teamwork.
5. Type comments and click **Save**.
6. The table refreshes, the new evaluation appears with its auto-calculated score (e.g. 4.50 / 5.0), and the **Performance Reports** tab charts update automatically.
7. Select a row and click **Edit Selected** to change a rating — the score recalculates. Click **Delete Selected** to remove it (a confirmation dialog appears first).

**What to say:** *"The score is derived from the four criteria — it can never disagree with them. The dialog only offers employees on this manager's team, so a manager can't evaluate someone outside their team. Every write is scoped to the signed-in manager's id in SQL."*

---

### Step 3 — Manager dashboard: Goal Setting tab

1. Switch to the **Goal Setting** tab.
2. Click **Set New Goal**.
3. Pick an employee from the team dropdown, type a title and description, choose a target date (must be in the future), and click **Save Goal**.
4. The new goal appears in the table with status **Pending**.
5. Use **Edit Selected** to change the title, date or status (Pending → In Progress → Completed).

**What to say:** *"Goals live in one table shared by managers and employees — the only difference is the `is_personal` flag. A target date in the past is rejected (GoalDeadlineException), so the data stays valid."*

---

### Step 4 — Manager dashboard: Feedback Provision tab

1. Switch to the **Feedback Provision** tab.
2. Click **Give Feedback**.
3. Pick an employee, choose a rating 1–5, type comments and click **Submit Feedback**.
4. The feedback appears in the table with the date.

**What to say:** *"Feedback can optionally be linked to an evaluation. The rating is validated to be between 1 and 5, both in the Java code and in the database (CHECK constraint)."*

---

### Step 5 — Manager dashboard: Performance Reports tab

1. Switch to the **Performance Reports** tab.
2. Two charts appear:
   - **Bar chart** — average score per employee on the manager's team.
   - **Pie chart** — team criteria averages (Communication, Technical, Punctuality, Teamwork).
3. Click **Export Team Report to CSV** and save a file. Open it in Excel or a text editor to show the data.

**What to say:** *"The aggregation happens inside MySQL (GROUP BY) — only the averages travel across the network, not every row. The CSV export follows RFC 4180 and guards against spreadsheet formula injection, so opening it in Excel is safe."*

---

### Step 6 — Manager dashboard: Team Management tab

1. Switch to the **Team Management** tab.
2. The table lists team members with name, role, department, designation and e-mail.
3. Click **Add Member to Team** — pick a team and an employee, type a role (e.g. "Lead Developer") and save.
4. Click **Assign Role** on an existing member to change their role with a single SQL UPDATE (atomic — no partial updates).
5. Use **Refresh** to reload from the database.

**What to say:** *"Teams and memberships are many-to-many through the `team_members` junction table. Changing a role is one atomic statement, so membership is never left half-updated."*

---

### Step 7 — Employee dashboard (log out, log in as an employee)

1. Click **Logout** in the manager dashboard.
2. Log in as `priya@company.com` / `emp123` with role **EMPLOYEE**.
3. The **Employee Dashboard** opens.

#### Performance Tracking tab
- A line chart shows the employee's score over time.
- Below it, the evaluation history table lists each evaluation with its score.
- The running average is shown at the top (e.g. "Average Score: 4.25 / 5.0").

**What to say:** *"This is the employee's own data only — filtered by the signed-in employee's id in SQL."*

#### Feedback History tab
- Every feedback item the employee has received, with the manager id, rating, comments and date.

#### Personal Goals tab
- Click **Set Personal Goal** to create a goal for themselves (title, description, target date).
- Use **Mark Complete** to move a goal to **Completed**.
- Use **Edit Selected** or **Delete Selected** to manage goals.
- A pie chart on the right shows the status distribution (Pending / In Progress / Completed).

**What to say:** *"An employee can only touch their own goals. Trying to read, edit or delete another employee's goal by guessing its id fails — the SQL adds `AND assigned_to = ?` on every query."*

#### Performance Reports tab
- Bar and pie charts of the employee's own criteria averages.
- Click **Export My Evaluations to CSV** to save a personal report.

---

### Step 8 — Logout and return to login

Click **Logout** from either dashboard — the window closes and the login screen reappears, ready for the next user.

---

## 4. Core Java concepts demonstrated (for the viva / marks)

| Concept | Where it is in the code |
|---|---|
| **Inheritance** | `User` (abstract) → `Manager`, `Employee` (`src/model/User.java`, `Manager.java`, `Employee.java`) |
| **Polymorphism** | `getDashboardType()` overridden in `Manager` and `Employee`; `Evaluation.calculateScore()` overloaded (no-arg and weighted 4-arg) |
| **Encapsulation** | All model fields are private with getters/setters; validation in constructors |
| **Abstraction / Interfaces** | Generic `DAO<T>` interface (`src/dao/DAO.java`); `AuthService`-style authentication in `UserDAO` |
| **Exception Handling** | Custom `InvalidRatingException`, `GoalDeadlineException`; try-with-resources on every JDBC block; `ExecutionException` unwrapping in `SwingWorker.done()` |
| **Collections & Generics** | `List<Evaluation>`, `Map<Integer, Double>`, `HashMap`, `ArrayList`, `DAO<T>` generic interface |
| **Multithreading** | `SwingWorker` for all database loading (UI never freezes); debounced 300 ms search timer; bounded `ConnectionPool` with `ArrayBlockingQueue` |
| **JDBC** | `DatabaseConnection` singleton + `ConnectionPool`; every statement is a `PreparedStatement` with bound parameters (no SQL injection) |
| **File I/O** | CSV export via `ReportExporter`; audit logging via `FileLogger` |
| **GUI / Event handling** | Swing `JFrame`, `JTabbedPane`, `JTable`, `JComboBox`, `JFileChooser`; `FlatLaf` look-and-feel |

---

## 5. Database design (show the ER diagram)

Open `docs/ER_DIAGRAM.md` or the `Review2_Final_Performetrics.pptx` to show the diagram. Six tables:

| Table | Purpose |
|---|---|
| `users` | Managers and employees (polymorphic by `role`) |
| `evaluations` | Performance ratings (4 criteria + overall score) |
| `goals` | Manager-assigned and personal goals (`is_personal` flag) |
| `feedback` | Manager → employee feedback with rating |
| `teams` | Teams owned by a manager |
| `team_members` | Many-to-many link between teams and employees |

**Key constraints:** ratings are `NOT NULL` + `CHECK (1-5)`; passwords are `VARCHAR(255)` to fit PBKDF2 hashes; foreign keys with `ON DELETE CASCADE`.

Setup order (from README):
```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed_data.sql
mysql -u root -p < db/migration_v2.sql
```
Then copy `db.properties.example` to `db.properties` and set your MySQL password.

---

## 6. Demo credentials and how to change a password

| Role | E-mail | Password |
|---|---|---|
| Manager | manager@company.com | manager123 |
| Employee | priya@company.com | emp123 |
| Employee | amit@company.com | emp123 |
| Employee | sneha@company.com | emp123 |

To set a new password for any account:
```bash
java -cp "bin;lib/*" util.PasswordHashTool "MyNewPassword"
```
This prints a PBKDF2 hash; paste it into an `UPDATE users SET password = ... WHERE email = ...` statement.

---

## 7. How to verify everything works (without the GUI)

If MySQL is running with seed data loaded:

```bash
# 20 end-to-end checks — login, ownership rules, password hashing
java -cp "bin;lib/*" tools.SmokeTest

# 66 unit + integration tests (skips DB tests if MySQL is down)
run-tests.bat
```

Expected output: all checks pass, all 66 tests pass.

---

## 8. Project structure (what to point at in VS Code)

```
src/
  App.java                 # Entry point: FlatLaf, DB check, login router
  model/                   # User (abstract), Manager, Employee, Evaluation, Goal, Feedback, Team
  dao/                     # DAO<T>, DatabaseConnection (singleton), ConnectionPool, *DAO
  service/                 # ReportService — reporting rules
  ui/                      # LoginFrame, ManagerDashboard, EmployeeDashboard
  ui/components/           # ChartPanel — JFreeChart wrapper (bar, pie, line)
  util/                    # PasswordUtil (PBKDF2), Validator, ReportExporter, FileLogger, Icons, PasswordHashTool
  exception/               # InvalidRatingException, GoalDeadlineException
  tools/                   # SmokeTest, ScreenshotTool
  test/java/               # JUnit 5 suite — 66 tests
db/
  schema.sql               # Tables, foreign keys, CHECK constraints
  seed_data.sql            # Demo accounts and data
  migration_v2.sql         # PBKDF2 migration + NOT NULL constraints
lib/
  flatlaf-3.4.1.jar, jfreechart-1.5.4.jar, mysql-connector-j-8.0.33.jar, junit jar
docs/
  ARCHITECTURE.md, ER_DIAGRAM.md, AUDIT_REPORT.md, screenshots/, PPTX files
```

---

## 9. Screenshots you can show (already captured)

| Screenshot | File | What it shows |
|---|---|---|
| Login | `docs/screenshots/01-login.png` | Email, password, role dropdown |
| Manager dashboard | `docs/screenshots/02-manager-dashboard.png` | 5 tabs, team header, tables |
| Manager reports | `docs/screenshots/03-manager-reports.png` | Bar chart + pie chart with real names |
| Employee dashboard | `docs/screenshots/04-employee-dashboard.png` | Tracking chart, goals pie chart |
| Employee reports | `docs/screenshots/05-employee-reports.png` | Own criteria averages |

These were captured from the live running app with `src/tools/ScreenshotTool.java`.

---

## 10. If something goes wrong during the demo

| Problem | Likely cause | Fix |
|---|---|---|
| "MySQL driver missing" | `lib/mysql-connector-j-8.0.33.jar` not on classpath | Re-run `run.bat` or verify `lib/` is in the VS Code classpath |
| Can't connect to database | MySQL not running, or wrong password in `db.properties` | Start MySQL (`start-mysql.bat`), then edit `db.properties` |
| Login fails with correct credentials | Seed data not loaded, or `migration_v2.sql` not run | Run the three SQL scripts in order from README |
| Charts blank | No evaluations for that user | Add an evaluation as the manager first |
| `UnsupportedClassVersionError` | Compiled with one JDK, running with another | Use `run.bat`/`run-tests.bat` — they use `JAVA_HOME` for both |

---

## 11. Closing line for the teacher

> *"Performetrics is a complete Core Java desktop application: role-based Swing dashboards, a pooled MySQL connection layer, PBKDF2 password security, SQL-enforced access control, live JFreeChart analytics and CSV reporting — covered by 66 automated tests."*

---

**Files to have open during the demo:** `src/App.java`, `src/ui/LoginFrame.java`, `src/ui/ManagerDashboard.java`, `src/ui/EmployeeDashboard.java`, `src/dao/DatabaseConnection.java`, `src/model/Evaluation.java`, `docs/ER_DIAGRAM.md`, `README.md`.

Good luck with the demonstration!