package model;

import java.time.LocalDate;

/**
 * Goal model - for Manager Goal Setting & Employee Personal Goals
 */
public class Goal {
    private int goalId;
    private String title;
    private String description;
    private int assignedTo;   // employeeId
    private int assignedBy;   // managerId or self
    private LocalDate targetDate;
    private String status;    // Pending, In Progress, Completed
    private boolean isPersonal; // true if employee personal goal
    private LocalDate createdAt;

    public Goal() {
        this.status = "Pending";
        this.createdAt = LocalDate.now();
    }

    public Goal(String title, String description, int assignedTo, int assignedBy, LocalDate targetDate, boolean isPersonal) {
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.targetDate = targetDate;
        this.isPersonal = isPersonal;
        this.status = "Pending";
        this.createdAt = LocalDate.now();
    }

    // Getters & Setters
    public int getGoalId() { return goalId; }
    public void setGoalId(int goalId) { this.goalId = goalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getAssignedTo() { return assignedTo; }
    public void setAssignedTo(int assignedTo) { this.assignedTo = assignedTo; }
    public int getAssignedBy() { return assignedBy; }
    public void setAssignedBy(int assignedBy) { this.assignedBy = assignedBy; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isPersonal() { return isPersonal; }
    public void setPersonal(boolean personal) { isPersonal = personal; }
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Goal{id=" + goalId + ", title='" + title + "', status=" + status + ", target=" + targetDate + "}";
    }
}
