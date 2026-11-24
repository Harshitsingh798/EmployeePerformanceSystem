package model;

import exception.InvalidRatingException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Feedback given by a manager to an employee, optionally linked to an
 * evaluation.
 */
public class Feedback {

    private int feedbackId;
    private int employeeId;
    private int managerId;
    private Integer evalId; // nullable - linked evaluation
    private String comments;
    private int rating; // 1-5
    private LocalDate givenOn;

    /**
     * Creates an empty feedback, used when mapping a database row.
     */
    public Feedback() {
        this.givenOn = LocalDate.now();
    }

    /**
     * Creates a fully populated feedback.
     *
     * @param employeeId the employee receiving the feedback
     * @param managerId  the manager giving the feedback
     * @param comments   the written feedback
     * @param rating     the rating, 1-5
     * @throws InvalidRatingException if the rating falls outside 1-5
     */
    public Feedback(int employeeId, int managerId, String comments, int rating) throws InvalidRatingException {
        Evaluation.validateRating(rating);
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.comments = comments;
        this.rating = rating;
        this.givenOn = LocalDate.now();
    }

    public int getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(int feedbackId) {
        this.feedbackId = feedbackId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public int getManagerId() {
        return managerId;
    }

    public void setManagerId(int managerId) {
        this.managerId = managerId;
    }

    public Integer getEvalId() {
        return evalId;
    }

    public void setEvalId(Integer evalId) {
        this.evalId = evalId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public LocalDate getGivenOn() {
        return givenOn;
    }

    public void setGivenOn(LocalDate givenOn) {
        this.givenOn = givenOn;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Feedback that)) {
            return false;
        }
        return feedbackId == that.feedbackId && feedbackId != 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackId);
    }

    @Override
    public String toString() {
        return "Feedback{id=" + feedbackId + ", emp=" + employeeId + ", rating=" + rating + "}";
    }
}
