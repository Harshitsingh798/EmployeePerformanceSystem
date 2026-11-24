package model;

import exception.InvalidRatingException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A performance evaluation of one employee by one manager.
 *
 * <p>The overall score is <em>always</em> derived from the four criteria, so it
 * can never drift out of sync with the individual ratings. The
 * {@code overall_score} column in the database is likewise written from
 * {@link #calculateScore()}.
 */
public class Evaluation {

    /** Lowest accepted rating for any criterion. */
    public static final int MIN_RATING = 1;

    /** Highest accepted rating for any criterion. */
    public static final int MAX_RATING = 5;

    private int evalId;
    private int employeeId;
    private int managerId;
    private String employeeName; // populated for display only
    private int communication;   // 1-5
    private int technical;       // 1-5
    private int punctuality;     // 1-5
    private int teamwork;        // 1-5
    private String comments;
    private LocalDate evalDate;

    /**
     * Creates an empty evaluation, used when mapping a database row.
     */
    public Evaluation() {
        this.evalDate = LocalDate.now();
    }

    /**
     * Creates a fully populated evaluation.
     *
     * @param employeeId   the evaluated employee
     * @param managerId    the evaluating manager
     * @param communication communication rating, 1-5
     * @param technical    technical rating, 1-5
     * @param punctuality  punctuality rating, 1-5
     * @param teamwork     teamwork rating, 1-5
     * @param comments     free-text comments
     * @throws InvalidRatingException if any rating falls outside 1-5
     */
    public Evaluation(int employeeId, int managerId, int communication, int technical,
                      int punctuality, int teamwork, String comments) throws InvalidRatingException {
        validateRating(communication);
        validateRating(technical);
        validateRating(punctuality);
        validateRating(teamwork);
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.communication = communication;
        this.technical = technical;
        this.punctuality = punctuality;
        this.teamwork = teamwork;
        this.comments = comments;
        this.evalDate = LocalDate.now();
    }

    /**
     * Calculates the unweighted average of the four criteria.
     *
     * @return the overall score between 1.0 and 5.0
     */
    public double calculateScore() {
        return (communication + technical + punctuality + teamwork) / 4.0;
    }

    /**
     * Calculates a weighted score.
     *
     * <p>This overload demonstrates method overloading. The weights are
     * normalised, so the result stays on the same 1-5 scale regardless of
     * whether they add up to 1.0.
     *
     * @param weightComm weight for communication
     * @param weightTech weight for technical skill
     * @param weightPunct weight for punctuality
     * @param weightTeam weight for teamwork
     * @return the weighted score between 1.0 and 5.0
     */
    public double calculateScore(double weightComm, double weightTech, double weightPunct, double weightTeam) {
        double total = weightComm + weightTech + weightPunct + weightTeam;
        if (total == 0.0) {
            return calculateScore();
        }
        return (communication * weightComm + technical * weightTech
                + punctuality * weightPunct + teamwork * weightTeam) / total;
    }

    /**
     * @return the overall score, always derived from the four criteria
     */
    public double getOverallScore() {
        return calculateScore();
    }

    /**
     * Checks that a rating is inside the permitted range.
     *
     * @param rating the rating to check
     * @throws InvalidRatingException when the rating is outside 1-5
     */
    public static void validateRating(int rating) throws InvalidRatingException {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new InvalidRatingException(
                    "Rating must be between " + MIN_RATING + " and " + MAX_RATING + ". Got: " + rating);
        }
    }

    public int getEvalId() {
        return evalId;
    }

    public void setEvalId(int evalId) {
        this.evalId = evalId;
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

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public int getCommunication() {
        return communication;
    }

    public void setCommunication(int communication) {
        this.communication = communication;
    }

    public int getTechnical() {
        return technical;
    }

    public void setTechnical(int technical) {
        this.technical = technical;
    }

    public int getPunctuality() {
        return punctuality;
    }

    public void setPunctuality(int punctuality) {
        this.punctuality = punctuality;
    }

    public int getTeamwork() {
        return teamwork;
    }

    public void setTeamwork(int teamwork) {
        this.teamwork = teamwork;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDate getEvalDate() {
        return evalDate;
    }

    public void setEvalDate(LocalDate evalDate) {
        this.evalDate = evalDate;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Evaluation that)) {
            return false;
        }
        return evalId == that.evalId && evalId != 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(evalId);
    }

    @Override
    public String toString() {
        return "Evaluation{id=" + evalId + ", emp=" + employeeId
                + ", score=" + String.format(java.util.Locale.ROOT, "%.2f", calculateScore())
                + ", date=" + evalDate + "}";
    }
}
