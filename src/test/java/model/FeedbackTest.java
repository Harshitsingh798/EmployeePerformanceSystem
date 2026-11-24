package model;

import exception.InvalidRatingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link Feedback}.
 */
@DisplayName("Feedback")
class FeedbackTest {

    @Test
    @DisplayName("feedback keeps the rating it was given")
    void storesTheRating() throws InvalidRatingException {
        Feedback feedback = new Feedback(2, 1, "Great sprint", 5);
        assertEquals(5, feedback.getRating());
        assertEquals(2, feedback.getEmployeeId());
        assertEquals(1, feedback.getManagerId());
        assertEquals("Great sprint", feedback.getComments());
    }

    @Test
    @DisplayName("a rating outside 1-5 is rejected")
    void rejectsOutOfRangeRating() {
        assertThrows(InvalidRatingException.class, () -> new Feedback(2, 1, "Too high", 6));
        assertThrows(InvalidRatingException.class, () -> new Feedback(2, 1, "Too low", 0));
    }

    @Test
    @DisplayName("the boundary ratings 1 and 5 are accepted")
    void acceptsBoundaryRatings() throws InvalidRatingException {
        assertEquals(1, new Feedback(2, 1, "Lowest", 1).getRating());
        assertEquals(5, new Feedback(2, 1, "Highest", 5).getRating());
    }

    @Test
    @DisplayName("the linked evaluation id is optional")
    void evaluationIdIsOptional() throws InvalidRatingException {
        Feedback feedback = new Feedback(2, 1, "No evaluation linked", 3);
        assertEquals(null, feedback.getEvalId());
        feedback.setEvalId(11);
        assertEquals(11, feedback.getEvalId());
    }

    @Test
    @DisplayName("equality is based on the persisted id")
    void equalityIsBasedOnId() {
        Feedback first = new Feedback();
        first.setFeedbackId(5);
        Feedback second = new Feedback();
        second.setFeedbackId(5);
        assertEquals(first, second);
        assertNotEquals(first, new Feedback());
    }
}
