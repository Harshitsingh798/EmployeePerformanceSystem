package model;

import exception.InvalidRatingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Evaluation}.
 */
@DisplayName("Evaluation")
class EvaluationTest {

    @Test
    @DisplayName("the overall score is the average of the four criteria")
    void calculatesOverallScore() throws InvalidRatingException {
        Evaluation evaluation = new Evaluation(2, 1, 4, 5, 4, 5, "Solid work");
        assertEquals(4.5, evaluation.calculateScore(), 0.001);
        assertEquals(4.5, evaluation.getOverallScore(), 0.001);
    }

    @Test
    @DisplayName("the weighted score is normalised to the same 1-5 scale")
    void calculatesWeightedScore() throws InvalidRatingException {
        Evaluation evaluation = new Evaluation(2, 1, 5, 5, 1, 1, "Mixed");
        // Weights sum to 10, not 1 - the result must still land on a 1-5 scale.
        // (5*4 + 5*3 + 1*2 + 1*1) / 10 = 3.8
        double weighted = evaluation.calculateScore(4.0, 3.0, 2.0, 1.0);
        assertEquals(3.8, weighted, 0.001);
    }

    @Test
    @DisplayName("weights that sum to zero fall back to the plain average")
    void weightedScoreFallsBackWhenWeightsAreZero() throws InvalidRatingException {
        Evaluation evaluation = new Evaluation(2, 1, 4, 4, 4, 4, "Even");
        assertEquals(4.0, evaluation.calculateScore(0, 0, 0, 0), 0.001);
    }

    @Test
    @DisplayName("a rating below 1 is rejected")
    void rejectsRatingBelowRange() {
        assertThrows(InvalidRatingException.class,
                () -> new Evaluation(2, 1, 0, 3, 3, 3, "Too low"));
    }

    @Test
    @DisplayName("a rating above 5 is rejected")
    void rejectsRatingAboveRange() {
        assertThrows(InvalidRatingException.class,
                () -> new Evaluation(2, 1, 3, 6, 3, 3, "Too high"));
    }

    @Test
    @DisplayName("the boundary ratings 1 and 5 are accepted")
    void acceptsBoundaryRatings() throws InvalidRatingException {
        Evaluation low = new Evaluation(2, 1, 1, 1, 1, 1, "Lowest");
        Evaluation high = new Evaluation(2, 1, 5, 5, 5, 5, "Highest");
        assertEquals(1.0, low.calculateScore(), 0.001);
        assertEquals(5.0, high.calculateScore(), 0.001);
    }

    @Test
    @DisplayName("validateRating reports the offending value")
    void validateRatingReportsTheValue() {
        InvalidRatingException error = assertThrows(InvalidRatingException.class,
                () -> Evaluation.validateRating(9));
        assertTrue(error.getMessage().contains("9"), "The message should name the bad value");
    }

    @Test
    @DisplayName("two evaluations with the same id are equal")
    void equalityIsBasedOnId() {
        Evaluation first = new Evaluation();
        first.setEvalId(7);
        Evaluation second = new Evaluation();
        second.setEvalId(7);
        Evaluation other = new Evaluation();
        other.setEvalId(8);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

    @Test
    @DisplayName("an evaluation that has not been persisted yet is not equal to another")
    void unsavedEvaluationsAreNotEqual() {
        assertNotEquals(new Evaluation(), new Evaluation());
    }
}
