package dao;

import model.Evaluation;
import model.Goal;
import model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for the data access layer.
 *
 * <p>These run against the real MySQL database, because the point of the
 * ownership rules is that they are enforced <em>in the SQL</em> - a mock
 * would not prove that. If the database is not reachable the tests are skipped
 * rather than failed, so the suite still passes on a machine with no MySQL.
 *
 * <p>Every test cleans up the rows it creates.
 */
@DisplayName("DAO (integration)")
class DaoIntegrationTest {

    /** The manager id used by the seed data. */
    private static final int MANAGER_ID = 1;

    /** The employee id used by the seed data. */
    private static final int EMPLOYEE_ID = 2;

    /** Whether the database was reachable at start-up. */
    private static boolean databaseAvailable;

    @BeforeAll
    static void connect() {
        databaseAvailable = DatabaseConnection.getInstance().testConnection();
        if (!databaseAvailable) {
            System.out.println("Skipping the DAO integration tests - no database is reachable.");
        }
    }

    @AfterAll
    static void disconnect() {
        if (databaseAvailable) {
            DatabaseConnection.getInstance().closeConnection();
        }
    }

    @Test
    @DisplayName("the demo manager can sign in")
    void managerCanAuthenticate() throws SQLException {
        assumeTrue(databaseAvailable);
        User manager = new UserDAO().authenticate("manager@company.com", "manager123", "MANAGER");
        assertNotNull(manager);
        assertEquals("MANAGER", manager.getRole());
        assertEquals(MANAGER_ID, manager.getUserId());
    }

    @Test
    @DisplayName("a wrong password is rejected")
    void wrongPasswordIsRejected() throws SQLException {
        assumeTrue(databaseAvailable);
        assertNull(new UserDAO().authenticate("manager@company.com", "wrong", "MANAGER"));
    }

    @Test
    @DisplayName("the wrong role is rejected")
    void wrongRoleIsRejected() throws SQLException {
        assumeTrue(databaseAvailable);
        assertNull(new UserDAO().authenticate("manager@company.com", "manager123", "EMPLOYEE"));
    }

    @Test
    @DisplayName("an evaluation can be written, read back and deleted")
    void evaluationRoundTrip() throws SQLException {
        assumeTrue(databaseAvailable);
        EvaluationDAO dao = new EvaluationDAO();

        Evaluation evaluation = new Evaluation();
        evaluation.setEmployeeId(EMPLOYEE_ID);
        evaluation.setManagerId(MANAGER_ID);
        evaluation.setCommunication(4);
        evaluation.setTechnical(3);
        evaluation.setPunctuality(5);
        evaluation.setTeamwork(4);
        evaluation.setComments("Round trip test");

        assertTrue(dao.add(evaluation), "The evaluation should be inserted");
        assertTrue(evaluation.getEvalId() > 0, "The generated id should be set");

        Evaluation stored = dao.getById(evaluation.getEvalId());
        assertNotNull(stored);
        assertEquals(4, stored.getCommunication());
        assertEquals(4.0, stored.calculateScore(), 0.001);

        assertTrue(dao.deleteByManager(evaluation.getEvalId(), MANAGER_ID));
        assertNull(dao.getById(evaluation.getEvalId()));
    }

    @Test
    @DisplayName("an evaluation can only be deleted by the manager who wrote it")
    void onlyTheAuthorCanDeleteAnEvaluation() throws SQLException {
        assumeTrue(databaseAvailable);
        EvaluationDAO dao = new EvaluationDAO();

        Evaluation evaluation = new Evaluation();
        evaluation.setEmployeeId(EMPLOYEE_ID);
        evaluation.setManagerId(MANAGER_ID);
        evaluation.setCommunication(3);
        evaluation.setTechnical(3);
        evaluation.setPunctuality(3);
        evaluation.setTeamwork(3);
        evaluation.setComments("Ownership test");
        assertTrue(dao.add(evaluation));

        // A different manager must not be able to remove it.
        assertFalse(dao.deleteByManager(evaluation.getEvalId(), MANAGER_ID + 99));
        assertNotNull(dao.getById(evaluation.getEvalId()));

        assertTrue(dao.deleteByManager(evaluation.getEvalId(), MANAGER_ID));
    }

    @Test
    @DisplayName("evaluations are scoped to the employee who owns them")
    void evaluationsAreScopedToTheEmployee() throws SQLException {
        assumeTrue(databaseAvailable);
        List<Evaluation> evaluations = new EvaluationDAO().getByEmployeeId(EMPLOYEE_ID);
        assertFalse(evaluations.isEmpty(), "The seed data should contain evaluations");
        assertTrue(evaluations.stream().allMatch(e -> e.getEmployeeId() == EMPLOYEE_ID));
    }

    @Test
    @DisplayName("evaluations are scoped to the manager who wrote them")
    void evaluationsAreScopedToTheManager() throws SQLException {
        assumeTrue(databaseAvailable);
        List<Evaluation> evaluations = new EvaluationDAO().getByManagerId(MANAGER_ID);
        assertFalse(evaluations.isEmpty());
        assertTrue(evaluations.stream().allMatch(e -> e.getManagerId() == MANAGER_ID));
    }

    @Test
    @DisplayName("a search finds the seeded employee by name")
    void searchFindsAnEmployee() throws SQLException {
        assumeTrue(databaseAvailable);
        List<Evaluation> results = new EvaluationDAO().search("Priya", MANAGER_ID);
        assertFalse(results.isEmpty(), "The search should match the seeded employee");
        assertTrue(results.stream().allMatch(e -> e.getManagerId() == MANAGER_ID));
    }

    @Test
    @DisplayName("a goal can only be read by its owner")
    void goalOwnershipIsEnforced() throws SQLException {
        assumeTrue(databaseAvailable);
        GoalDAO dao = new GoalDAO();

        Goal goal = new Goal();
        goal.setTitle("Ownership test goal");
        goal.setDescription("Created by the test");
        goal.setAssignedTo(EMPLOYEE_ID);
        goal.setAssignedBy(MANAGER_ID);
        goal.setTargetDate(java.time.LocalDate.now().plusDays(30));
        goal.setStatus(Goal.STATUS_PENDING);
        goal.setPersonal(false);
        assertTrue(dao.add(goal));

        assertNotNull(dao.getOwnedGoal(goal.getGoalId(), EMPLOYEE_ID), "The owner can read it");
        assertNull(dao.getOwnedGoal(goal.getGoalId(), EMPLOYEE_ID + 99), "A stranger cannot read it");
        assertFalse(dao.deleteOwnedGoal(goal.getGoalId(), EMPLOYEE_ID + 99),
                "A stranger cannot delete it");
        assertTrue(dao.deleteOwnedGoal(goal.getGoalId(), EMPLOYEE_ID));
    }

    @Test
    @DisplayName("the average score is computed in the database")
    void averageScoreIsComputed() throws SQLException {
        assumeTrue(databaseAvailable);
        double average = new EvaluationDAO().getAverageScore(EMPLOYEE_ID);
        assertTrue(average >= 1.0 && average <= 5.0,
                "The average should land on the 1-5 scale, got " + average);
    }

    @Test
    @DisplayName("the team roster lists the manager's employees")
    void teamRosterIsScoped() throws SQLException {
        assumeTrue(databaseAvailable);
        List<model.Employee> team = new UserDAO().getEmployeesForManager(MANAGER_ID);
        assertFalse(team.isEmpty(), "The seed data should contain a team");
        assertTrue(team.stream().allMatch(u -> u.getManagerId() == MANAGER_ID));
    }

    @Test
    @DisplayName("the team members query returns the seeded roster")
    void teamMembersAreListed() throws SQLException {
        assumeTrue(databaseAvailable);
        List<Object[]> members = new TeamDAO().getMembers(1);
        assertFalse(members.isEmpty(), "The seed data should contain a team");
        for (Object[] member : members) {
            assertEquals(3, member.length, "Each row should be [userId, name, role]");
        }
    }

    @Test
    @DisplayName("the pool hands out and takes back connections")
    void connectionPoolIsUsable() throws SQLException {
        assumeTrue(databaseAvailable);
        try (Connection first = DatabaseConnection.getInstance().getConnection();
             Connection second = DatabaseConnection.getInstance().getConnection()) {
            assertFalse(first.isClosed());
            assertFalse(second.isClosed());
            try (Statement statement = first.createStatement()) {
                statement.execute("SELECT 1");
            }
        }
        // The pool must not leak connections.
        assertTrue(DatabaseConnection.getInstance().getPool().getOpenConnectionCount() <= 5,
                "The pool should never hold more than its maximum size");
    }
}
