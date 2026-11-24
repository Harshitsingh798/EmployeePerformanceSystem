package exception;

/**
 * Raised when a rating falls outside the permitted 1-5 range.
 */
public class InvalidRatingException extends Exception {

    /** Serialisation version - required for a {@link java.io.Serializable} subclass. */
    private static final long serialVersionUID = 1L;

    /**
     * @param message a human readable explanation of the problem
     */
    public InvalidRatingException(String message) {
        super(message);
    }
}
