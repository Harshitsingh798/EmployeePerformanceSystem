package model;

import exception.GoalDeadlineException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Goal}.
 */
@DisplayName("Goal")
class GoalTest {

    @Test
    @DisplayName("a new goal starts out pending")
    void newGoalIsPending() throws GoalDeadlineException {
        Goal goal = new Goal("Ship v2", "Finish the release", 2, 1,
                LocalDate.now().plusDays(7), false);
        assertEquals(Goal.STATUS_PENDING, goal.getStatus());
        assertFalse(goal.isPersonal());
    }

    @Test
    @DisplayName("a goal with a target date in the past is rejected")
    void rejectsPastTargetDate() {
        assertThrows(GoalDeadlineException.class,
                () -> new Goal("Too late", "Already overdue", 2, 1,
                        LocalDate.now().minusDays(1), false));
    }

    @Test
    @DisplayName("a goal with today's date is rejected - the deadline must be in the future")
    void rejectsTodayAsTargetDate() {
        assertThrows(GoalDeadlineException.class,
                () -> new Goal("Due today", "Not strictly in the future", 2, 1,
                        LocalDate.now(), false));
    }

    @Test
    @DisplayName("a goal with a missing target date is rejected")
    void rejectsMissingTargetDate() {
        assertThrows(GoalDeadlineException.class,
                () -> new Goal("No date", "No deadline", 2, 1, null, false));
    }

    @Test
    @DisplayName("a personal goal is flagged as personal")
    void personalGoalIsFlagged() throws GoalDeadlineException {
        Goal goal = new Goal("Learn Kotlin", "Self study", 2, 2,
                LocalDate.now().plusDays(30), true);
        assertTrue(goal.isPersonal());
    }

    @Test
    @DisplayName("only the three defined statuses are accepted")
    void onlyKnownStatusesAreValid() {
        assertTrue(Goal.isValidStatus(Goal.STATUS_PENDING));
        assertTrue(Goal.isValidStatus(Goal.STATUS_IN_PROGRESS));
        assertTrue(Goal.isValidStatus(Goal.STATUS_COMPLETED));
        assertFalse(Goal.isValidStatus("Cancelled"));
        assertFalse(Goal.isValidStatus(null));
        assertFalse(Goal.isValidStatus(""));
    }

    @Test
    @DisplayName("equality is based on the persisted id")
    void equalityIsBasedOnId() {
        Goal first = new Goal();
        first.setGoalId(3);
        Goal second = new Goal();
        second.setGoalId(3);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
