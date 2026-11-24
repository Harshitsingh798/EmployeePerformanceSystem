package util;

import exception.GoalDeadlineException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Validator}.
 */
@DisplayName("Validator")
class ValidatorTest {

    /** A fixed clock, so "today" means the same thing on every run. */
    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    @AfterEach
    void restoreTheSystemClock() {
        Validator.useClock(null);
    }

    @Test
    @DisplayName("a rating inside 1-5 is accepted")
    void acceptsValidRating() {
        assertDoesNotThrow(() -> Validator.validateRating(1));
        assertDoesNotThrow(() -> Validator.validateRating(3));
        assertDoesNotThrow(() -> Validator.validateRating(5));
    }

    @Test
    @DisplayName("a rating outside 1-5 is rejected")
    void rejectsInvalidRating() {
        assertThrows(Exception.class, () -> Validator.validateRating(0));
        assertThrows(Exception.class, () -> Validator.validateRating(6));
    }

    @Test
    @DisplayName("isValidScore reports the range without throwing")
    void reportsValidScores() {
        assertTrue(Validator.isValidScore(1));
        assertTrue(Validator.isValidScore(5));
        assertFalse(Validator.isValidScore(0));
        assertFalse(Validator.isValidScore(6));
    }

    @Test
    @DisplayName("a future deadline is accepted")
    void acceptsFutureDeadline() {
        Validator.useClock(FIXED);
        assertDoesNotThrow(() -> Validator.validateGoalDeadline(LocalDate.of(2026, 6, 16)));
    }

    @Test
    @DisplayName("today's date is not a valid deadline")
    void rejectsTodayAsDeadline() {
        Validator.useClock(FIXED);
        assertThrows(GoalDeadlineException.class,
                () -> Validator.validateGoalDeadline(LocalDate.of(2026, 6, 15)));
    }

    @Test
    @DisplayName("a past date is not a valid deadline")
    void rejectsPastDeadline() {
        Validator.useClock(FIXED);
        assertThrows(GoalDeadlineException.class,
                () -> Validator.validateGoalDeadline(LocalDate.of(2026, 6, 14)));
    }

    @Test
    @DisplayName("a missing deadline is rejected")
    void rejectsMissingDeadline() {
        assertThrows(GoalDeadlineException.class, () -> Validator.validateGoalDeadline(null));
    }

    @Test
    @DisplayName("a plausible e-mail address is accepted")
    void acceptsValidEmail() {
        assertDoesNotThrow(() -> Validator.validateEmail("priya@company.com"));
        assertDoesNotThrow(() -> Validator.validateEmail("  priya@company.com  "));
    }

    @Test
    @DisplayName("a malformed e-mail address is rejected")
    void rejectsInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> Validator.validateEmail("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> Validator.validateEmail("a@b"));
        assertThrows(IllegalArgumentException.class, () -> Validator.validateEmail("@company.com"));
        assertThrows(IllegalArgumentException.class, () -> Validator.validateEmail(""));
        assertThrows(IllegalArgumentException.class, () -> Validator.validateEmail(null));
    }

    @Test
    @DisplayName("blank text is rejected and the field name is reported")
    void rejectsBlankText() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Validator.requireText("   ", "Title"));
        assertTrue(error.getMessage().contains("Title"));
        assertThrows(IllegalArgumentException.class, () -> Validator.requireText(null, "Title"));
    }

    @Test
    @DisplayName("non-blank text is accepted")
    void acceptsNonBlankText() {
        assertDoesNotThrow(() -> Validator.requireText("Ship v2", "Title"));
    }
}
