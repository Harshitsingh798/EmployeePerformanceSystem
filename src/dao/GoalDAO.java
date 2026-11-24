package dao;

import model.Goal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the {@code goals} table.
 *
 * <p>The table stores both manager-assigned goals and employee personal goals;
 * the {@code is_personal} flag distinguishes them.
 */
public class GoalDAO implements DAO<Goal> {

    /** Explicit column list. */
    private static final String COLUMNS =
            "goal_id, title, description, assigned_to, assigned_by, target_date, status, is_personal, created_at";

    @Override
    public boolean add(Goal g) throws SQLException {
        String sql = "INSERT INTO goals (title, description, assigned_to, assigned_by, target_date, status, is_personal) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, g.getTitle());
            ps.setString(2, g.getDescription());
            ps.setInt(3, g.getAssignedTo());
            ps.setInt(4, g.getAssignedBy());
            ps.setDate(5, Date.valueOf(g.getTargetDate()));
            ps.setString(6, g.getStatus());
            ps.setBoolean(7, g.isPersonal());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        g.setGoalId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public Goal getById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM goals WHERE goal_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Loads a goal only when the given employee is allowed to see it.
     *
     * <p>This is the ownership check used by the employee dashboard. Without
     * it, an employee could read any goal in the database simply by guessing
     * its id.
     *
     * @param id         the goal id
     * @param employeeId the employee making the request
     * @return the goal, or {@code null} when it does not exist or is not theirs
     * @throws SQLException if the query fails
     */
    public Goal getOwnedGoal(int id, int employeeId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM goals WHERE goal_id = ? AND assigned_to = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Goal> getAll() throws SQLException {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM goals ORDER BY target_date ASC, goal_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Loads every goal assigned to an employee - both the manager's goals and
     * the employee's own personal goals.
     *
     * @param employeeId the employee's user id
     * @return the goals, ordered by target date
     * @throws SQLException if the query fails
     */
    public List<Goal> getByEmployee(int employeeId) throws SQLException {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM goals WHERE assigned_to = ? ORDER BY target_date ASC, goal_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Loads only the employee's own personal goals.
     *
     * @param employeeId the employee's user id
     * @return the personal goals, ordered by target date
     * @throws SQLException if the query fails
     */
    public List<Goal> getPersonalGoals(int employeeId) throws SQLException {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM goals WHERE assigned_to = ? AND is_personal = TRUE "
                + "ORDER BY target_date ASC, goal_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Loads the goals a manager has assigned.
     *
     * @param managerId the manager's user id
     * @return the goals, ordered by target date
     * @throws SQLException if the query fails
     */
    public List<Goal> getAssignedByManager(int managerId) throws SQLException {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM goals WHERE assigned_by = ? ORDER BY target_date ASC, goal_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public boolean update(Goal g) throws SQLException {
        String sql = "UPDATE goals SET title=?, description=?, target_date=?, status=? WHERE goal_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, g.getTitle());
            ps.setString(2, g.getDescription());
            ps.setDate(3, Date.valueOf(g.getTargetDate()));
            ps.setString(4, g.getStatus());
            ps.setInt(5, g.getGoalId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates a goal only when the given employee owns it.
     *
     * @param goal       the goal to update
     * @param employeeId the employee making the request
     * @return {@code true} when the row was changed
     * @throws SQLException if the update fails
     */
    public boolean updateOwnedGoal(Goal goal, int employeeId) throws SQLException {
        String sql = "UPDATE goals SET title=?, description=?, target_date=?, status=? "
                + "WHERE goal_id=? AND assigned_to=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.getTitle());
            ps.setString(2, goal.getDescription());
            ps.setDate(3, Date.valueOf(goal.getTargetDate()));
            ps.setString(4, goal.getStatus());
            ps.setInt(5, goal.getGoalId());
            ps.setInt(6, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Changes the status of a goal, optionally restricted to its owner.
     *
     * @param goalId     the goal id
     * @param status     the new status
     * @param employeeId when greater than zero, only goals owned by that employee are updated
     * @return {@code true} when the row was changed
     * @throws SQLException if the update fails
     */
    public boolean updateStatus(int goalId, String status, int employeeId) throws SQLException {
        String sql = "UPDATE goals SET status=? WHERE goal_id=?" + (employeeId > 0 ? " AND assigned_to=?" : "");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, goalId);
            if (employeeId > 0) {
                ps.setInt(3, employeeId);
            }
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM goals WHERE goal_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a goal only when the given employee owns it.
     *
     * @param id         the goal id
     * @param employeeId the employee making the request
     * @return {@code true} when the row was removed
     * @throws SQLException if the delete fails
     */
    public boolean deleteOwnedGoal(int id, int employeeId) throws SQLException {
        String sql = "DELETE FROM goals WHERE goal_id = ? AND assigned_to = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row onto a {@link Goal}.
     */
    private Goal mapRow(ResultSet rs) throws SQLException {
        Goal goal = new Goal();
        goal.setGoalId(rs.getInt("goal_id"));
        goal.setTitle(rs.getString("title"));
        goal.setDescription(rs.getString("description"));
        goal.setAssignedTo(rs.getInt("assigned_to"));
        goal.setAssignedBy(rs.getInt("assigned_by"));
        Date targetDate = rs.getDate("target_date");
        if (targetDate != null) {
            goal.setTargetDate(targetDate.toLocalDate());
        }
        goal.setStatus(rs.getString("status"));
        goal.setPersonal(rs.getBoolean("is_personal"));
        Date createdAt = rs.getDate("created_at");
        if (createdAt != null) {
            goal.setCreatedAt(createdAt.toLocalDate());
        }
        return goal;
    }
}
