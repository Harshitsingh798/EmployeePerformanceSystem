package exception;

/**
 * Custom exception for invalid goal deadlines (must be future date)
 */
public class GoalDeadlineException extends Exception {
    public GoalDeadlineException(String message) {
        super(message);
    }
}
