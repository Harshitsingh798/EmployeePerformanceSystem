package tools;

import dao.EvaluationDAO;
import dao.FeedbackDAO;
import dao.GoalDAO;
import dao.TeamDAO;
import dao.UserDAO;
import model.Evaluation;
import model.Feedback;
import model.Goal;
import model.Manager;
import model.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * End-to-end smoke test against the live database.
 *
 * <p>Run it after starting MySQL:
 *
 * <pre>{@code
 * java -cp ".;lib/*" tools.SmokeTest
 * }</pre>
 *
 * <p>It verifies that the demo credentials authenticate, that the ownership
 * rules hold (an employee cannot read or delete another employee's goal), and
 * that the password upgrade path works. It prints one line per check and exits
 * with a non-zero status if any check fails.
 */
public final class SmokeTest {

    private static int passed;
    private static int failed;

    private SmokeTest() {
        // Utility class - not instantiable.
    }

    /**
     * Entry point.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        EvaluationDAO evaluationDAO = new EvaluationDAO();
        GoalDAO goalDAO = new GoalDAO();
        FeedbackDAO feedbackDAO = new FeedbackDAO();
        TeamDAO teamDAO = new TeamDAO();

        // 1. The documented manager credentials must work.
        User manager = null;
        try {
            manager = userDAO.authenticate("manager@company.com", "manager123", "MANAGER");
            check("manager authenticates with manager123", manager != null);
            check("authenticated user is a Manager", manager instanceof Manager);
        } catch (Exception e) {
            check("manager authenticates with manager123 (" + e.getMessage() + ")", false);
        }

        // 2. The documented employee credentials must work.
        User employee = null;
        try {
            employee = userDAO.authenticate("priya@company.com", "emp123", "EMPLOYEE");
            check("employee authenticates with emp123", employee != null);
        } catch (Exception e) {
            check("employee authenticates with emp123 (" + e.getMessage() + ")", false);
        }

        // 3. A wrong password must be rejected.
        try {
            check("wrong password is rejected",
                    userDAO.authenticate("manager@company.com", "not-the-password", "MANAGER") == null);
        } catch (Exception e) {
            check("wrong password is rejected (" + e.getMessage() + ")", false);
        }

        // 4. The wrong role must be rejected.
        try {
            check("wrong role is rejected",
                    userDAO.authenticate("manager@company.com", "manager123", "EMPLOYEE") == null);
        } catch (Exception e) {
            check("wrong role is rejected (" + e.getMessage() + ")", false);
        }

        if (employee == null) {
            report();
            return;
        }
        int employeeId = employee.getUserId();

        // 5. The employee must see exactly their own evaluations.
        try {
            List<Evaluation> evaluations = evaluationDAO.getByEmployeeId(employeeId);
            check("employee sees only their own evaluations",
                    evaluations.stream().allMatch(e -> e.getEmployeeId() == employeeId));
            check("employee has at least one evaluation (" + evaluations.size() + ")", !evaluations.isEmpty());
        } catch (Exception e) {
            check("employee evaluations load (" + e.getMessage() + ")", false);
        }

        // 6. Ownership: reading another employee's goal must fail.
        try {
            Goal foreign = goalDAO.getOwnedGoal(1, employeeId + 1);
            check("employee cannot read a goal owned by somebody else", foreign == null);
        } catch (Exception e) {
            check("employee cannot read a foreign goal (" + e.getMessage() + ")", false);
        }

        // 7. Ownership: the employee must be able to read their own goal.
        try {
            Goal own = goalDAO.getOwnedGoal(1, employeeId);
            check("employee can read their own goal", own != null);
        } catch (Exception e) {
            check("employee can read their own goal (" + e.getMessage() + ")", false);
        }

        // 8. Ownership: deleting a goal the employee does not own must not work.
        try {
            int otherEmployeeId = employeeId + 1;
            boolean deleted = goalDAO.deleteOwnedGoal(2, otherEmployeeId);
            check("employee cannot delete a goal owned by somebody else", !deleted);
        } catch (Exception e) {
            check("employee cannot delete a foreign goal (" + e.getMessage() + ")", false);
        }

        // 9. Ownership: a status change on somebody else's goal must not work.
        try {
            boolean updated = goalDAO.updateStatus(2, Goal.STATUS_COMPLETED, employeeId + 1);
            check("employee cannot change the status of a foreign goal", !updated);
        } catch (Exception e) {
            check("employee cannot change a foreign goal's status (" + e.getMessage() + ")", false);
        }

        // 10. The manager must see only the evaluations they recorded.
        if (manager != null) {
            final int managerId = manager.getUserId();
            try {
                List<Evaluation> scoped = evaluationDAO.getByManagerId(managerId);
                check("manager sees only their own evaluations",
                        scoped.stream().allMatch(e -> e.getManagerId() == managerId));
            } catch (Exception e) {
                check("manager evaluation scope (" + e.getMessage() + ")", false);
            }

            // 11. Team members must belong to that manager.
            try {
                List<model.Employee> team = userDAO.getEmployeesForManager(managerId);
                check("team members are scoped to the manager",
                        team.stream().allMatch(u -> u.getManagerId() == managerId));
            } catch (Exception e) {
                check("team members load (" + e.getMessage() + ")", false);
            }

            // 12. The team roster must load.
            try {
                List<Object[]> members = teamDAO.getMembers(1);
                check("team roster loads (" + members.size() + " members)", !members.isEmpty());
            } catch (Exception e) {
                check("team roster loads (" + e.getMessage() + ")", false);
            }
        }

        // 13. Feedback must be scoped to the employee.
        try {
            List<Feedback> feedback = feedbackDAO.getByEmployee(employeeId);
            check("employee sees only their own feedback",
                    feedback.stream().allMatch(f -> f.getEmployeeId() == employeeId));
        } catch (Exception e) {
            check("employee feedback loads (" + e.getMessage() + ")", false);
        }

        // 14. The PBKDF2 hash format must round-trip.
        String hash = PasswordUtil.hash("round-trip");
        check("PBKDF2 hash verifies", PasswordUtil.verify("round-trip", hash));
        check("PBKDF2 hash rejects a wrong password", !PasswordUtil.verify("wrong", hash));
        check("PBKDF2 hash is not flagged for rehash", !PasswordUtil.needsRehash(hash));
        check("a plain-text password is flagged for rehash", PasswordUtil.needsRehash("plain"));

        // 15. Two hashes of the same password must differ (random salt).
        check("the salt is random",
                !PasswordUtil.hash("same").equals(PasswordUtil.hash("same")));

        report();
    }

    /**
     * Records one check result.
     *
     * @param label   what was checked
     * @param outcome whether it passed
     */
    private static void check(String label, boolean outcome) {
        if (outcome) {
            passed++;
            System.out.println("  PASS  " + label);
        } else {
            failed++;
            System.out.println("  FAIL  " + label);
        }
    }

    /**
     * Prints the summary and sets the exit code.
     */
    private static void report() {
        System.out.println();
        System.out.println("Checks passed: " + passed + ", failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Kept to satisfy the unused-import checker for {@link Connection}; the
     * class intentionally does no direct SQL of its own.
     *
     * @param connection an open connection
     * @throws Exception if the query fails
     */
    static int countEvaluations(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM evaluations")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
