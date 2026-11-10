# Performetrics - Employee Performance Evaluation System

> GUVI HCL Hackathon | Problem #18 | Core Java (Swing + JDBC + JFreeChart)

A desktop application for managers to evaluate employee performance, set goals, provide feedback, and generate reports — while employees track progress, view feedback, and manage personal goals.

## Features

### Manager Dashboard
- **Performance Evaluation** - Table listing evaluations with criteria (Communication, Technical, Punctuality, Teamwork) and auto-calculated score
- **Goal Setting** - Set goals for employees with target dates
- **Feedback Provision** - Give feedback with ratings and comments
- **Performance Reports** - Graphical representation (JFreeChart bar/pie) of team performance
- **Team Management** - Manage team members and assign roles

### Employee Dashboard
- **Performance Tracking** - Line chart showing progress over time
- **Feedback History** - List of feedback received from managers
- **Personal Goals** - Set and track personal goals with status
- **Performance Reports** - Pie chart breakdown + CSV export

## Tech Stack
- **Language:** Java 17
- **UI:** Java Swing + FlatLaf (modern look)
- **Database:** MySQL 8.0 + JDBC (mysql-connector-j)
- **Charts:** JFreeChart 1.5.x
- **Security:** SHA-256 password hashing
- **Concepts:** OOP, Collections & Generics, Exception Handling, File I/O, Multithreading (SwingWorker)

## Project Structure
```
src/
├── App.java                 # Entry point with FlatLaf theme
├── model/                   # User, Manager, Employee, Evaluation, Goal, Feedback, Team
├── dao/                     # DAO<T> interface, DatabaseConnection (Singleton), *DAO
├── service/                 # ReportService
├── ui/                      # LoginFrame, ManagerDashboard, EmployeeDashboard
│   └── components/          # ChartPanel (JFreeChart wrapper)
├── util/                    # DBConfig, FileLogger, ReportExporter, PasswordUtil, Validator
└── exception/               # InvalidRatingException, GoalDeadlineException
db/
├── schema.sql               # Database schema
├── seed_data.sql            # Sample data
└── migration_v2.sql         # Hash passwords
lib/                         # JAR dependencies
```

## How to Run

### Prerequisites
- JDK 17+
- MySQL 8.0
- VS Code with Extension Pack for Java

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/Harshitsingh798/EmployeePerformanceSystem.git
   ```
2. Create database:
   ```bash
   mysql -u root -p < db/schema.sql
   mysql -u root -p < db/seed_data.sql
   mysql -u root -p < db/migration_v2.sql
   ```
   Or run `schema.sql`, `seed_data.sql`, `migration_v2.sql` in MySQL Workbench.

3. Configure database (optional):
   Edit `db.properties`:
   ```
   db.url=jdbc:mysql://localhost:3306/employee_performance_db
   db.user=root
   db.password=your_password
   ```

4. Add dependencies to `lib/`:
   - `mysql-connector-j-8.0.33.jar`
   - `jfreechart-1.5.4.jar`
   - `flatlaf-3.4.1.jar`

5. Run:
   - Open in VS Code → Run `src/App.java`
   - Or: `javac -cp "lib/*" -d bin -sourcepath src src/App.java && java -cp "bin;lib/*" App`

### Demo Credentials
- **Manager:** `manager@company.com` / `manager123`
- **Employee:** `priya@company.com` / `emp123`

## Database Design
ER: `users (MANAGER/EMPLOYEE) 1:N evaluations, goals, feedback | teams M:N users`
See `db/schema.sql` for full schema.

## License
Academic project for GUVI Hackathon Review 2.
