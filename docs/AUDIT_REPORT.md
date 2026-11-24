# Code Audit Report — Performetrics (Employee Performance Evaluation System)

An audit of the project against the GUVI Review 2 criteria: code quality,
execution, testing, and the security and documentation expectations of the
problem statement. Every issue below was found in the code, fixed, and
verified — the "fixed" column records how it was verified.

**Summary:** 25 issues found, 25 fixed, 0 outstanding.

---

## Critical issues (fixed)

### 1. The documented demo passwords did not work after the documented setup

**Severity:** Critical · **Where:** `db/migration_v2.sql`

The migration replaced every password with the same hash — the SHA-256 of the
literal string `password`:

```sql
-- before
UPDATE users SET password = '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8'
 WHERE email = 'manager@company.com' AND password = 'manager123';
```

`sha256("password")` is exactly that value, but `sha256("manager123")` is
`866485796cfa8d7c0cf7111640205b83076433547577511d81f8030ae99ecea5`. So after
running the setup that README tells a reviewer to run, the credentials
`manager123` / `emp123` in the README were dead — every account would have had
the password `password`.

**Fixed by** replacing the migration with real PBKDF2-HMAC-SHA256 hashes, and
adding `src/util/PasswordHashTool.java`, which generates them:

```
java -cp "bin;lib/*" util.PasswordHashTool "MyPassword"
```

**Verified:** the migration was applied to a live MySQL instance and the
seeded accounts authenticate with the documented passwords (see
`src/tools/SmokeTest.java`, checks 1–4).

---

### 2. Passwords were stored as a plain SHA-256 digest

**Severity:** Critical · **Where:** `src/util/PasswordUtil.java`

A bare SHA-256 digest is fast to compute, so a stolen table can be cracked
with billions of guesses per second on a GPU. It also carries no salt, so two
users with the same password have the same hash and one rainbow table breaks
both.

**Fixed by** switching to PBKDF2-HMAC-SHA256 with 210 000 iterations (the
OWASP recommendation), a random 16-byte salt per password, and a constant-time
comparison:

```java
private static final int ITERATIONS = 210_000;

public static String hash(String raw) {
    byte[] salt = new byte[SALT_LENGTH_BYTES];
    RANDOM.nextBytes(salt);
    byte[] derived = derive(raw.toCharArray(), salt, ITERATIONS);
    return String.join("$", PREFIX, String.valueOf(ITERATIONS),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(derived));
}
```

The format is self-describing — `pbkdf2$<iterations>$<salt>$<hash>` — so the
iteration count can be raised later without a schema change. Legacy
plain-text and SHA-256 rows still verify and are silently upgraded on the
first sign-in, so nobody is locked out.

**Verified:** `PasswordUtilTest` (12 tests) covers round-tripping, the random
salt, the legacy paths and malformed input; the live database was migrated
and the accounts still work.

---

### 3. An employee could read or delete another employee's goals

**Severity:** Critical · **Where:** `src/ui/EmployeeDashboard.java`, `src/dao/GoalDAO.java`

The employee dashboard fetched goals by id alone:

```java
// before
Goal goal = new GoalDAO().getById(id);       // no ownership check
new GoalDAO().delete(id);
new GoalDAO().updateStatus(id, "Completed");
```

Any employee who guessed or edited a goal id could read it, delete it, or
mark somebody else's goal complete.

**Fixed by** adding ownership-scoped queries to `GoalDAO` and using them
everywhere in the employee dashboard:

```java
public Goal getOwnedGoal(int id, int employeeId) throws SQLException {
    String sql = "SELECT " + COLUMNS + " FROM goals WHERE goal_id = ? AND assigned_to = ?";
    ...
}

public boolean deleteOwnedGoal(int id, int employeeId) throws SQLException { ... }
public boolean updateOwnedGoal(Goal goal, int employeeId) throws SQLException { ... }
public boolean updateStatus(int goalId, String status, int employeeId) throws SQLException { ... }
```

**Verified:** `DaoIntegrationTest.goalOwnershipIsEnforced` and three checks in
`SmokeTest` assert that a stranger can neither read, delete nor re-status a
goal.

---

### 4. A shared `Connection` was closed by the first DAO that finished

**Severity:** Critical · **Where:** `src/dao/DatabaseConnection.java`

Every DAO method opened the *same* `Connection` object through
try-with-resources. The first DAO to finish closed it, and every other DAO
running at the same time failed with `No operations allowed after connection
closed`. Both dashboards start five concurrent loaders on open, so this
happened on every launch.

**Fixed by** introducing `src/dao/ConnectionPool.java` — a bounded
`ArrayBlockingQueue` of connections, with a `java.lang.reflect.Proxy` that
intercepts `close()` and returns the connection to the pool instead of closing
it. A connection whose statement threw a `SQLException` is discarded rather
than reused.

**Verified:** `DaoIntegrationTest.connectionPoolIsUsable` borrows two
connections at once and asserts the pool never exceeds its maximum size.

---

### 5. The employee's Performance Tracking chart was always blank

**Severity:** Critical · **Where:** `src/ui/EmployeeDashboard.java`

`createTrackingPanel()` assigned the chart slot to the field `lineSlot`, and
`createReportsPanel()` then **reassigned the same field** to a different
panel:

```java
// before
lineSlot = L1;   // in createTrackingPanel()
lineSlot = L2;   // in createReportsPanel() - overwrites it
```

The trend chart was rendered into the Reports tab, and the Performance
Tracking tab showed nothing at all.

**Fixed by** giving each chart its own field — `trackingChartSlot`,
`reportsPieSlot`, `reportsBarSlot` and `goalStatusSlot` — so no slot can be
overwritten.

**Verified:** the screenshot in `docs/screenshots/04-employee-dashboard.png`
shows the line chart on the Performance Tracking tab.

---

### 6. SQL exceptions were caught in blocks where they cannot be thrown

**Severity:** Critical · **Where:** `src/ui/ManagerDashboard.java`,
`src/ui/LoginFrame.java`, `src/ui/EmployeeDashboard.java`

A `SwingWorker`'s `done()` method receives failures wrapped in an
`ExecutionException`, so `catch (SQLException e)` there was unreachable —
and it hid the real error from the user, because the wrapper's message is
just `java.sql.SQLException: ...`.

**Fixed by** catching `ExecutionException` and unwrapping the cause:

```java
} catch (ExecutionException e) {
    Throwable cause = e.getCause() == null ? e : e.getCause();
    JOptionPane.showMessageDialog(this,
            "Could not reach the database.\n" + cause.getMessage(), ...);
    FileLogger.logError("Login failed for " + email, (Exception) cause);
}
```

**Verified:** the whole tree compiles with `-Xlint:all` and reports **zero
errors and zero warnings**.

---

## High severity (fixed)

### 7. The chart used a `Date` where the API wanted a `double`

**Severity:** High · **Where:** `src/ui/components/ChartPanel.java`

`series.add(Date, Double)` does not compile — `XYSeries.add` takes a `double`
x-value. The chart code was also mixing a `Date` x-axis with `LocalDate`
data, so two evaluations on the same day overwrote each other.

**Fixed by** converting to epoch milliseconds and using a real `DateAxis`, and
by adding a marker at every point so a single evaluation is still visible:

```java
double millis = date.atStartOfDay(zone).toInstant().toEpochMilli();
series.add(millis, scores.get(i));
plot.setDomainAxis(new DateAxis("Date"));
```

---

### 8. The manager's report chart showed `Emp #2` instead of names

**Severity:** High · **Where:** `src/ui/ManagerDashboard.java`

The chart resolved employee names from the `teamMembers` list, which is
populated by a *different* background task. The report task often ran first
and fell back to the id.

**Fixed by** resolving the names inside the report task itself, from the same
database query, so the labels never depend on a race.

**Verified:** `docs/screenshots/03-manager-reports.png` shows
"Priya Sharma", "Amit Patel" and "Sneha Gupta" on the x-axis.

---

### 9. Raw ids were typed into the manager's dialogs

**Severity:** High · **Where:** `src/ui/ManagerDashboard.java`

The evaluation, goal and feedback dialogs asked the manager to type an
employee id by hand — easy to get wrong, and it let a manager aim a write at
an employee outside their team.

**Fixed by** replacing the text fields with `JComboBox` pickers populated
from `getEmployeesForManager(managerId)`, so the only ids on offer are the
manager's own team.

---

### 10. Rating columns could be `NULL`

**Severity:** High · **Where:** `db/schema.sql`

A `CHECK (rating BETWEEN 1 AND 5)` constraint still permits `NULL`, because
the comparison evaluates to unknown rather than false. A `NULL` rating would
silently poison the score calculation.

**Fixed by** making the columns `NOT NULL` as well as `CHECK`-constrained, in
both `schema.sql` and `migration_v2.sql`.

---

### 11. The password column was too narrow for a PBKDF2 hash

**Severity:** High · **Where:** `db/schema.sql`

`password VARCHAR(100)` is too small for a self-describing PBKDF2 hash once
the iteration count grows. **Fixed by** widening it to `VARCHAR(255)` in both
`schema.sql` and the migration.

---

### 12. There was no test suite at all

**Severity:** High · **Where:** the whole project

**Fixed by** adding **66 JUnit 5 tests** under `src/test/java`:

| Test class | Tests | Covers |
|---|---|---|
| `model/EvaluationTest` | 9 | score calculation, weighted score, rating bounds, equality |
| `model/GoalTest` | 7 | deadline validation, status vocabulary, equality |
| `model/FeedbackTest` | 5 | rating validation, optional evaluation link |
| `util/PasswordUtilTest` | 12 | hashing, salting, legacy formats, malformed input |
| `util/ValidatorTest` | 10 | ratings, deadlines, e-mail, blank text |
| `util/ReportExporterTest` | 8 | RFC 4180 quoting, formula-injection defence, file output |
| `dao/DaoIntegrationTest` | 15 | authentication, CRUD, **ownership scoping**, aggregation, the pool |

`run-tests.bat` runs them with **no Maven required**; `mvn test` runs exactly
the same tests. The DAO tests skip cleanly when no database is reachable.

---

### 13. No build system or dependency management

**Severity:** High · **Where:** project root

**Fixed by** adding `pom.xml` (Maven, JUnit 5, compiler with `-Xlint:all`, and
a shade plugin that produces a runnable jar) plus `run-tests.bat` and
`run.bat` for people without Maven.

---

## Medium severity (fixed)

### 14. The search fired a query on every keystroke

**Severity:** Medium · **Where:** `src/ui/ManagerDashboard.java`

**Fixed by** debouncing the search by 300 ms with a `Timer` and cancelling the
previous `SwingWorker` when a new one starts, so only the last query of a
burst actually runs.

### 15. The report charts loaded every row into Java to average them

**Severity:** Medium · **Where:** `src/dao/EvaluationDAO.java`

**Fixed by** adding `getAverageScoresByEmployee(int scope)` and
`getAverageScore(int employeeId)`, which do the aggregation in SQL with a
`GROUP BY`, so only the averages travel over the wire.

### 16. `FileLogger` opened and closed the file for every line

**Severity:** Medium · **Where:** `src/util/FileLogger.java`

**Fixed by** keeping a single `BufferedWriter` open, guarded by
`synchronized`, and closing it from a shutdown hook.

### 17. The manager could see evaluations written by other managers

**Severity:** Medium · **Where:** `src/dao/EvaluationDAO.java`

**Fixed by** adding `getByManagerId(int)`, `search(String, int)` and
`deleteByManager(int, int)`, and scoping every manager query to the signed-in
manager's id.

### 18. `TeamDAO` updated a member's role with two separate statements

**Severity:** Medium · **Where:** `src/dao/TeamDAO.java`

**Fixed by** replacing it with a single atomic `UPDATE ... SET role = ?`.

### 19. Dead code and unused fields

**Severity:** Medium · **Where:** throughout

`util/DBConfig.java` (a duplicate of the connection settings),
`Icons.getIcon`, the unused `overallScore` field on `Evaluation` and the
unused `managerName` field were removed or wired in. The three window classes
are now `final`, which also cleared the last `this-escape` compiler warnings.

### 20. `ReportService` was not scoped and threw bare `Exception`

**Severity:** Medium · **Where:** `src/service/ReportService.java`

**Fixed by** rewriting it with `throws SQLException, IOException` and adding
`exportManagerReportCSV(int, String)` and `exportEmployeeReportCSV(int, String)`,
so a manager can only ever export their own team's evaluations.

### 21. The CSV exporter was vulnerable to spreadsheet formula injection

**Severity:** Medium · **Where:** `src/util/ReportExporter.java`

A comment of `=SUM(A1:A9)` would be executed by Excel when the exported file
was opened.

**Fixed by** prefixing any value that starts with `=`, `+`, `-` or `@` with an
apostrophe, collapsing embedded line breaks, doubling quotes, and writing a
UTF-8 BOM so Excel reads the file correctly.

---

## Found in the final consistency pass (fixed)

### 22. `db.properties` was tracked in git despite being in `.gitignore`

**Severity:** High · **Where:** repository root, `src/`

`.gitignore` listed `db.properties`, but the file was already **tracked**, and
`.gitignore` only affects untracked files. A reviewer who edited the file to
put in their real MySQL password would have seen it as a modified file in
`git status` — and could easily have committed it to the public repository.

**Fixed by** removing both files from the index with `git rm --cached
db.properties src/db.properties`. They stay on disk, still ignored, and
`db.properties.example` remains the template.

**Verified:** `git ls-files` no longer lists either file, and `git status`
shows no pending change to them. The committed versions had an empty
password, so **no credential was ever exposed** — but a local edit would
have been.

### 23. The model layer wrote to the console

**Severity:** Medium · **Where:** `src/model/User.java`, `src/model/Employee.java`, `src/model/Manager.java`

`displayInfo()` was an abstract method whose only job was to
`System.out.println(...)`, and nothing ever called it. A model class that
prints to the console breaks the layering the rest of the project is built
on, and it was dead code on top of that.

**Fixed by** replacing it with `describe()`, which returns a string, and
using that string in the sign-in audit log:

```java
@Override
public String describe() {
    return "Employee " + name + " (" + department + ", " + designation + ")";
}
```

```java
FileLogger.logInfo("Login", user.describe() + " signed in (id " + user.getUserId()
        + ") -> " + user.getDashboardType());
```

### 24. Unused constructors defaulted to a hardcoded user id

**Severity:** Medium · **Where:** `src/ui/ManagerDashboard.java`, `src/ui/EmployeeDashboard.java`

`ManagerDashboard(String)` and `EmployeeDashboard(String)` were leftover
"convenience" constructors that defaulted to manager id `1` and employee id
`2`:

```java
// before
public ManagerDashboard(String managerName) {
    this(managerName, 1);
}
```

Nothing called them, and a caller who did would have silently opened the
dashboard of the **wrong user** — id 1 is the seeded manager, so an employee
would have seen the manager's data. This is the kind of default that turns
into a data leak the moment somebody uses it.

**Fixed by** deleting both constructors. The only way to open a dashboard now
is with an explicit, signed-in user id.

### 25. The scripts could compile with one JDK and run with another

**Severity:** Medium · **Where:** `run.bat`, `run-tests.bat`

Both scripts called whatever `javac` and `java` happened to be on `PATH`. On
a machine with two JDKs installed — one newer, one older — the compile
succeeds and the run then dies with:

```
UnsupportedClassVersionError: ui/LoginFrame has been compiled by a more
recent version of the Java Runtime (class file version 69.0), this version
of the Java Runtime only recognizes class file versions up to 65.0
```

This was hit on the machine that built this project, so it is a real failure
and not a hypothetical one.

**Fixed by** taking both the compiler and the runtime from `JAVA_HOME` when
it is set, and falling back to `PATH` only when it is not.

**Verified:** both scripts run to completion, and the whole tree compiles
with `--release 17` as well as with the default target.

---

## Verification

| Check | Command | Result |
|---|---|---|
| Compile with every warning enabled | `javac -Xlint:all -cp "lib/*" -d bin -sourcepath src src/App.java` | **0 errors, 0 warnings** |
| Compile at the Maven target release | `javac --release 17 -Xlint:all ...` | **0 errors, 0 warnings** |
| Unit and integration tests | `run-tests.bat` | **66 tests, 66 passed, 0 failed** |
| End-to-end database checks | `java -cp "bin;lib/*" tools.SmokeTest` | **20 checks, 20 passed** |
| The application starts | `java -cp "bin;lib/*" App` | **Runs; 5 screenshots captured** |
| Live login | `manager@company.com` / `manager123` | **Works** |
| Live login | `priya@company.com` / `emp123` | **Works** |

---

## What is still open

Nothing that blocks the review. Two things are worth knowing:

- **The `lib/` jars are committed to the repository.** This keeps the project
  runnable without Maven, which is convenient for a reviewer. If the project
  moves to a pure-Maven build, `.gitignore` already excludes `*.jar` and the
  `pom.xml` fetches them instead.
- **`db.properties` is no longer tracked.** It used to be, despite being in
  `.gitignore` — see issue 22. It is now ignored properly and
  `db.properties.example` is the template. The committed copy had an empty
  password, so nothing was ever exposed, but the file has now been removed
  from the repository.
