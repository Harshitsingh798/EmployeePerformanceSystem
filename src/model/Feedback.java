package model;

import java.time.LocalDate;

/**
 * Feedback model - Manager gives feedback to Employee
 */
public class Feedback {
    private int feedbackId;
    private int employeeId;
    private int managerId;
    private Integer evalId; // nullable - linked evaluation
    private String comments;
    private int rating; // 1-5
    private LocalDate givenOn;

    public Feedback() {
        this.givenOn = LocalDate.now();
    }

    public Feedback(int employeeId, int managerId, String comments, int rating) {
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.comments = comments;
        this.rating = rating;
        this.givenOn = LocalDate.now();
    }

    // Getters & Setters
    public int getFeedbackId() { return feedbackId; }
    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }
    public Integer getEvalId() { return evalId; }
    public void setEvalId(Integer evalId) { this.evalId = evalId; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public LocalDate getGivenOn() { return givenOn; }
    public void setGivenOn(LocalDate givenOn) { this.givenOn = givenOn; }

    @Override
    public String toString() {
        return "Feedback{id=" + feedbackId + ", emp=" + employeeId + ", rating=" + rating + "}";
    }
}
