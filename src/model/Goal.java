package model;

import exception.GoalDeadlineException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * A goal, either assigned by a manager or set by an employee for themselves.
 *
 * <p>The {@code is_personal} flag distinguishes the two kinds; both live in the
 * same table so that progress can be reported on them uniformly.
 */
public class Goal {

    /** Status of a goal that has not been started. */
    public static final String STATUS_PENDING = "Pending";

    /** Status of a goal that is under way. */
    public static final String STATUS_IN_PROGRESS = "In Progress";

    /** Status of a goal that has been finished. */
    public static final String STATUS_COMPLETED = "Completed";

    /** Every status the database accepts. */
    public static final Set<String> VALID_STATUSES =
            Set.of(STATUS_PENDING, STATUS_IN_PROGRESS, STATUS_COMPLETED);

    private int goalId;
    private String title;
    private String description;
    private int assignedTo;   // employeeId
    private int assignedBy;   // managerId, or the employee themselves
    private LocalDate targetDate;
    private String status;
    private boolean personal;
    private LocalDate createdAt;

    /**
     * Creates an empty goal, used when mapping a database row.
     */
    public Goal() {
        this.status = STATUS_PENDING;
        this.createdAt = LocalDate.now();
    }

    /**
     * Creates a fully populated goal.
     *
     * @param title       the goal title
     * @param description a longer description
     * @param assignedTo  the employee the goal belongs to
     * @param assignedBy  the manager assigning the goal, or the employee for a personal goal
     * @param targetDate  the due date; must be in the future
     * @param personal    {@code true} for an employee's own goal
     * @throws GoalDeadlineException if the target date is missing or not in the future
     */
    public Goal(String title, String description, int assignedTo, int assignedBy,
                LocalDate targetDate, boolean personal) throws GoalDeadlineException {
        if (targetDate == null || !targetDate.isAfter(LocalDate.now())) {
            throw new GoalDeadlineException("Target date must be a future date. Got: " + targetDate);
        }
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.targetDate = targetDate;
        this.personal = personal;
        this.status = STATUS_PENDING;
        this.createdAt = LocalDate.now();
    }

    /**
     * Checks that a status is one the database accepts.
     *
     * @param status the status to check
     * @return {@code true} when the status is valid
     */
    public static boolean isValidStatus(String status) {
        return status != null && VALID_STATUSES.contains(status);
    }

    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(int assignedTo) {
        this.assignedTo = assignedTo;
    }

    public int getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(int assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPersonal() {
        return personal;
    }

    public void setPersonal(boolean personal) {
        this.personal = personal;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Goal that)) {
            return false;
        }
        return goalId == that.goalId && goalId != 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(goalId);
    }

    @Override
    public String toString() {
        return "Goal{id=" + goalId + ", title='" + title + "', status=" + status + ", target=" + targetDate + "}";
    }
}
