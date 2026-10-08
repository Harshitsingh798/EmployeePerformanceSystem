package exception;

/**
 * Custom exception for invalid ratings (must be 1-5)
 */
public class InvalidRatingException extends Exception {
    public InvalidRatingException(String message) {
        super(message);
    }
}
