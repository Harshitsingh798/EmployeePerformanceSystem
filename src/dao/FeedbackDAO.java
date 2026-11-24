package dao;

import model.Feedback;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the {@code feedback} table.
 */
public class FeedbackDAO implements DAO<Feedback> {

    /** Explicit column list. */
    private static final String COLUMNS =
            "feedback_id, employee_id, manager_id, eval_id, comments, rating, given_on";

    @Override
    public boolean add(Feedback f) throws SQLException {
        String sql = "INSERT INTO feedback (employee_id, manager_id, eval_id, comments, rating, given_on) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, f.getEmployeeId());
            ps.setInt(2, f.getManagerId());
            if (f.getEvalId() != null) {
                ps.setInt(3, f.getEvalId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, f.getComments());
            ps.setInt(5, f.getRating());
            ps.setDate(6, Date.valueOf(f.getGivenOn()));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        f.setFeedbackId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public Feedback getById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM feedback WHERE feedback_id = ?";
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

    @Override
    public List<Feedback> getAll() throws SQLException {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM feedback ORDER BY given_on DESC, feedback_id DESC";
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
     * Loads every feedback addressed to one employee.
     *
     * @param employeeId the employee's user id
     * @return the feedback rows, newest first
     * @throws SQLException if the query fails
     */
    public List<Feedback> getByEmployee(int employeeId) throws SQLException {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM feedback WHERE employee_id = ? "
                + "ORDER BY given_on DESC, feedback_id DESC";
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
     * Loads every feedback written by one manager.
     *
     * @param managerId the manager's user id
     * @return the feedback rows, newest first
     * @throws SQLException if the query fails
     */
    public List<Feedback> getByManagerId(int managerId) throws SQLException {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM feedback WHERE manager_id = ? "
                + "ORDER BY given_on DESC, feedback_id DESC";
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
    public boolean update(Feedback f) throws SQLException {
        String sql = "UPDATE feedback SET comments=?, rating=? WHERE feedback_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getComments());
            ps.setInt(2, f.getRating());
            ps.setInt(3, f.getFeedbackId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates feedback only when it was written by the given manager.
     *
     * @param feedback  the feedback to update
     * @param managerId the manager attempting the change
     * @return {@code true} when the row was changed
     * @throws SQLException if the update fails
     */
    public boolean updateByManager(Feedback feedback, int managerId) throws SQLException {
        String sql = "UPDATE feedback SET comments=?, rating=? WHERE feedback_id=? AND manager_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, feedback.getComments());
            ps.setInt(2, feedback.getRating());
            ps.setInt(3, feedback.getFeedbackId());
            ps.setInt(4, managerId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM feedback WHERE feedback_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes feedback only when it was written by the given manager.
     *
     * @param id        the feedback id
     * @param managerId the manager attempting the delete
     * @return {@code true} when the row was removed
     * @throws SQLException if the delete fails
     */
    public boolean deleteByManager(int id, int managerId) throws SQLException {
        String sql = "DELETE FROM feedback WHERE feedback_id=? AND manager_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, managerId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row onto a {@link Feedback}.
     */
    private Feedback mapRow(ResultSet rs) throws SQLException {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(rs.getInt("feedback_id"));
        feedback.setEmployeeId(rs.getInt("employee_id"));
        feedback.setManagerId(rs.getInt("manager_id"));
        int evalId = rs.getInt("eval_id");
        if (!rs.wasNull()) {
            feedback.setEvalId(evalId);
        }
        feedback.setComments(rs.getString("comments"));
        feedback.setRating(rs.getInt("rating"));
        Date givenOn = rs.getDate("given_on");
        if (givenOn != null) {
            feedback.setGivenOn(givenOn.toLocalDate());
        }
        return feedback;
    }
}
