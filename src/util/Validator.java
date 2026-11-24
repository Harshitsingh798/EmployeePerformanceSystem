package util;

import exception.GoalDeadlineException;
import exception.InvalidRatingException;
import model.Evaluation;

import java.time.Clock;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Input validation helpers.
 *
 * <p>Validation lives here rather than inside the Swing dialogs so that every
 * entry point - UI, service layer or a future API - enforces the same rules.
 */
public final class Validator {

    /** Pragmatic e-mail pattern - deliberately simple, not RFC 5322 complete. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** The clock used for date validation; overridable so tests are deterministic. */
    private static Clock clock = Clock.systemDefaultZone();

    private Validator() {
        // Utility class - not instantiable.
    }

    /**
     * Replaces the clock used by {@link #validateGoalDeadline(LocalDate)}.
     *
     * @param newClock the clock to use; passing {@code null} restores the system clock
     */
    public static void useClock(Clock newClock) {
        clock = newClock == null ? Clock.systemDefaultZone() : newClock;
    }

    /**
     * Checks that a rating is between 1 and 5.
     *
     * @param rating the rating to check
     * @throws InvalidRatingException when the rating is out of range
     */
    public static void validateRating(int rating) throws InvalidRatingException {
        Evaluation.validateRating(rating);
    }

    /**
     * Checks that a goal's due date lies in the future.
     *
     * @param targetDate the due date to check
     * @throws GoalDeadlineException when the date is missing or not in the future
     */
    public static void validateGoalDeadline(LocalDate targetDate) throws GoalDeadlineException {
        if (targetDate == null || !targetDate.isAfter(LocalDate.now(clock))) {
            throw new GoalDeadlineException("Target date must be a future date. Got: " + targetDate);
        }
    }

    /**
     * Checks that an e-mail address looks plausible.
     *
     * @param email the address to check
     * @throws IllegalArgumentException when the address is empty or malformed
     */
    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
    }

    /**
     * @param score the score to check
     * @return {@code true} when the score is between 1 and 5
     */
    public static boolean isValidScore(int score) {
        return score >= Evaluation.MIN_RATING && score <= Evaluation.MAX_RATING;
    }

    /**
     * Checks that a string is not blank.
     *
     * @param value the value to check
     * @param field the field name, used in the error message
     * @throws IllegalArgumentException when the value is null or blank
     */
    public static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
    }
}
