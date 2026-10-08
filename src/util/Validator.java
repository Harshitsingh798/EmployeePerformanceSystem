package util;

import exception.GoalDeadlineException;
import exception.InvalidRatingException;
import java.time.LocalDate;

/**
 * Validation utility - Exception Handling demo
 */
public class Validator {

    public static void validateRating(int rating) throws InvalidRatingException {
        if (rating < 1 || rating > 5) {
            throw new InvalidRatingException("Rating must be between 1 and 5. Got: " + rating);
        }
    }

    public static void validateGoalDeadline(LocalDate targetDate) throws GoalDeadlineException {
        if (targetDate == null || !targetDate.isAfter(LocalDate.now())) {
            throw new GoalDeadlineException("Target date must be a future date. Got: " + targetDate);
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
    }

    public static boolean isValidScore(int score) {
        return score >= 1 && score <= 5;
    }
}
