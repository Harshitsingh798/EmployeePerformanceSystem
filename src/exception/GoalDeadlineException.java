package exception;

/**
 * Raised when a goal's target date is missing or already in the past.
 */
public class GoalDeadlineException extends Exception {

    /** Serialisation version - required for a {@link java.io.Serializable} subclass. */
    private static final long serialVersionUID = 1L;

    /**
     * @param message a human readable explanation of the problem
     */
    public GoalDeadlineException(String message) {
        super(message);
    }
}
