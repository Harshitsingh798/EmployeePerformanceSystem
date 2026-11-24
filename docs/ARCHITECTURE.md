# Architecture

## Layers

```
┌─────────────────────────────────────────────────────────────────┐
│  ui/                                                            │
│  LoginFrame · ManagerDashboard · EmployeeDashboard · ChartPanel │
│  Swing windows, dialogs, tables, charts. No SQL, no business    │
│  rules - only presentation.                                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │ calls
┌───────────────────────────▼─────────────────────────────────────┐
│  service/                                                        │
│  ReportService - the reporting rules live here, so the windows   │
│  stay free of formatting code.                                   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ calls
┌───────────────────────────▼─────────────────────────────────────┐
│  dao/                                                            │
│  DAO<T> · UserDAO · EvaluationDAO · GoalDAO · FeedbackDAO       │
│  TeamDAO · DatabaseConnection (singleton) · ConnectionPool       │
│  Every statement is a PreparedStatement.                         │
└───────────────────────────┬─────────────────────────────────────┘
                            │ reads and writes
┌───────────────────────────▼─────────────────────────────────────┐
│  model/                                                          │
│  User · Manager · Employee · Evaluation · Goal · Feedback · Team │
│  Plain data with the invariants enforced in the constructors.     │
└─────────────────────────────────────────────────────────────────┘

  util/   PasswordUtil · Validator · ReportExporter · FileLogger · Icons
  exception/  InvalidRatingException · GoalDeadlineException
```

The rule is simple: **a layer may only call the layer below it.** The UI
never touches a DAO's SQL directly, and a DAO never knows that a UI exists.

---

## The generic DAO

All data access goes through one interface:

```java
public interface DAO<T> {
    boolean add(T entity) throws SQLException;
    T getById(int id) throws SQLException;
    List<T> getAll() throws SQLException;
    boolean update(T entity) throws SQLException;
    boolean delete(int id) throws SQLException;
}
```

`EvaluationDAO implements DAO<Evaluation>`, `GoalDAO implements DAO<Goal>` and
so on. The shared contract means a generic helper can work with any of them,
and the compiler catches a signature mistake at the call site.

Each concrete DAO then adds the queries that are specific to it — the
ownership-scoped reads, the aggregations, the search.

---

## The connection pool

`DatabaseConnection` is a singleton that owns a `ConnectionPool`.

```java
try (Connection conn = DatabaseConnection.getInstance().getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ...
}
```

The interesting part is the `try`-with-resources above. It calls
`conn.close()` when the block ends — but the connection must **not** really be
closed, because it should go back into the pool for the next caller.

`ConnectionPool` solves this with a `java.lang.reflect.Proxy`:

```java
// Inside the proxy's invocation handler:
if ("close".equals(method.getName())) {
    release(physicalConnection);   // return it to the pool
    return null;                   // do not actually close it
}
```

A second layer of safety: if a statement throws a `SQLException`, the handler
discards the physical connection instead of returning a possibly-broken one to
the pool.

The pool itself is an `ArrayBlockingQueue` with a bounded capacity, so it is
safe to use from several threads at once — which matters, because every tab of
both dashboards loads its data on its own `SwingWorker`.

---

## Threading

Swing is single-threaded: the event dispatch thread (EDT) must never block,
or the window freezes. Every database call therefore runs on a
`SwingWorker`:

```java
new SwingWorker<List<Evaluation>, Void>() {
    @Override
    protected List<Evaluation> doInBackground() {
        return new EvaluationDAO().getByEmployeeId(employeeId);  // off the EDT
    }

    @Override
    protected void done() {
        // Back on the EDT - safe to touch the table and the chart.
    }
}.execute();
```

Two details worth noting:

- **`done()` receives failures wrapped.** `get()` throws an
  `ExecutionException`, so the code unwraps it to recover the original
  `SQLException` and show the real reason. Catching `SQLException` directly in
  `done()` would not compile — the compiler knows it cannot be thrown there.
- **Search is debounced and cancellable.** Typing in the search box starts a
  300 ms timer; when it fires, the previous search worker is cancelled and a
  new one starts. Without this, every keystroke would fire a query.

---

## Authorization

Access control is enforced **in the SQL**, not in the interface:

```java
// GoalDAO
public Goal getOwnedGoal(int id, int employeeId) throws SQLException {
    String sql = "SELECT " + COLUMNS + " FROM goals WHERE goal_id = ? AND assigned_to = ?";
    ...
}
```

The employee's id comes from the signed-in session, never from a text field
the user can type into. A dialog that wants to know an employee id offers a
`JComboBox` populated from the manager's own team, so the value is always one
the user is allowed to use.

This is why the ownership rules are covered by integration tests against a real
database: a mock could not prove that the `WHERE` clause is there.

---

## Error handling

| Situation | How it is handled |
|---|---|
| Invalid rating (outside 1–5) | `InvalidRatingException` from the model constructor — the invalid object never exists |
| Goal deadline in the past | `GoalDeadlineException` from the model constructor |
| Database unreachable | The `SQLException` propagates to `done()`, is unwrapped, shown to the user and written to `app.log` |
| Bad input in a dialog | Caught, the message shown, the dialog left open so the user can correct it |
| Missing MySQL driver | Checked at start-up; a clear message is shown instead of a `ClassNotFoundException` |

The rule throughout: **never swallow an exception.** Every catch block either
recovers, or reports the real message.

---

## Password handling

`PasswordUtil` hashes a password with PBKDF2-HMAC-SHA256:

```
pbkdf2$<iterations>$<saltBase64>$<hashBase64>
```

- 210 000 iterations (the OWASP recommendation for this algorithm)
- a fresh random 16-byte salt per password, so two users with the same
  password do not share a hash
- a constant-time comparison, so the check does not leak how much of the
  password was guessed correctly

The format is self-describing, so the iteration count can be raised later
without a schema change: an old hash is detected by `needsRehash()` and
replaced the next time the user signs in.

Legacy plain-text and SHA-256 rows still verify, so no account is ever locked
out by the upgrade.

---

## Design decisions

**One `goals` table instead of two.**
Manager-assigned goals and employee personal goals share the same columns and
differ only by `is_personal`. One table means one set of queries and one
status vocabulary.

**The overall score is derived, never stored independently.**
`Evaluation.calculateScore()` computes it from the four criteria, and the
application writes that same value into the `overall_score` column. There is
no way for the two to disagree.

**Validation lives in `util/Validator`, not in the dialogs.**
The same rule then applies whether the data arrives from the UI, the service
layer or a future API.

**Charts live behind `ui/components/ChartPanel`.**
The dashboards ask for a bar, pie or line chart and never import JFreeChart
directly, so the charting library can be swapped without touching them.

**Time is injected where it matters.**
`Validator.useClock(Clock)` lets a test decide what "today" is, so a
deadline test is deterministic instead of depending on the day it runs.
