# Entity–Relationship Diagram

The database has **six tables**. The diagram below is the same schema that
`db/schema.sql` creates.

```mermaid
erDiagram
    USERS {
        int user_id PK "AUTO_INCREMENT"
        varchar name "NOT NULL"
        varchar email UK "NOT NULL"
        varchar password "NOT NULL - PBKDF2 hash"
        enum role "MANAGER | EMPLOYEE"
        varchar department
        varchar designation
        int manager_id FK "self-reference - who they report to"
        timestamp created_at
    }

    EVALUATIONS {
        int eval_id PK "AUTO_INCREMENT"
        int employee_id FK "NOT NULL"
        int manager_id FK "NOT NULL"
        int communication "NOT NULL, CHECK 1-5"
        int technical "NOT NULL, CHECK 1-5"
        int punctuality "NOT NULL, CHECK 1-5"
        int teamwork "NOT NULL, CHECK 1-5"
        double overall_score "derived from the four criteria"
        text comments
        date eval_date "DEFAULT CURRENT_DATE"
    }

    GOALS {
        int goal_id PK "AUTO_INCREMENT"
        varchar title "NOT NULL"
        text description
        int assigned_to FK "NOT NULL - the employee who owns it"
        int assigned_by FK "NOT NULL - the manager, or the employee"
        date target_date "NOT NULL"
        enum status "Pending | In Progress | Completed"
        boolean is_personal "FALSE = set by a manager"
        timestamp created_at
    }

    FEEDBACK {
        int feedback_id PK "AUTO_INCREMENT"
        int employee_id FK "NOT NULL"
        int manager_id FK "NOT NULL"
        int eval_id FK "NULLABLE - optional link"
        text comments "NOT NULL"
        int rating "NOT NULL, CHECK 1-5"
        date given_on "DEFAULT CURRENT_DATE"
    }

    TEAMS {
        int team_id PK "AUTO_INCREMENT"
        varchar team_name "NOT NULL"
        int manager_id FK "NOT NULL"
        timestamp created_at
    }

    TEAM_MEMBERS {
        int team_id PK, FK
        int employee_id PK, FK
        varchar role "Lead Developer | QA | Designer"
    }

    USERS ||--o{ EVALUATIONS : "is evaluated in"
    USERS ||--o{ EVALUATIONS : "writes"
    USERS ||--o{ GOALS : "owns"
    USERS ||--o{ GOALS : "assigns"
    USERS ||--o{ FEEDBACK : "receives"
    USERS ||--o{ FEEDBACK : "writes"
    USERS ||--o{ TEAMS : "manages"
    USERS ||--o{ USERS : "reports to"
    TEAMS ||--o{ TEAM_MEMBERS : "has"
    USERS ||--o{ TEAM_MEMBERS : "belongs to"
```

## Relationships in words

| Relationship | Cardinality | Meaning |
|---|---|---|
| `users` → `evaluations` | 1 : N | One employee has many evaluations; one manager writes many |
| `users` → `users` | 1 : N | A manager manages many employees, through `manager_id` |
| `users` → `goals` | 1 : N | A goal belongs to one employee (`assigned_to`) and was set by one person (`assigned_by`) |
| `users` → `feedback` | 1 : N | Feedback runs from a manager to an employee, optionally linked to an evaluation |
| `users` → `teams` | 1 : N | A manager owns the teams they created |
| `users` ↔ `teams` | N : N | Through the `team_members` junction table |

## Design notes

- **One `goals` table, not two.** Manager-assigned goals and employee personal
  goals share the same columns and differ only by the `is_personal` flag, so
  progress can be reported on them uniformly.
- **`assigned_to` and `assigned_by` are different.** A personal goal has the
  employee in both columns; a manager-assigned goal has the employee in
  `assigned_to` and the manager in `assigned_by`. Both columns are foreign
  keys with `ON DELETE CASCADE`, so removing a user removes their goals.
- **Ratings are `NOT NULL` as well as `CHECK`-constrained.** A `CHECK` alone
  still permits `NULL`, and a `NULL` rating would break the score calculation.
- **`overall_score` is always derived.** The application writes it from the
  four criteria so it can never disagree with them.
- **`team_members.role`** is a free-text column, because the role a person
  plays in a team (Lead Developer, QA, Designer) is team-specific rather than
  a property of the user.
