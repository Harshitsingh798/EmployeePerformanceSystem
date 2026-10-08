package dao;

import model.Feedback;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Feedback DAO - JDBC Implementation
 */
public class FeedbackDAO implements DAO<Feedback> {

    @Override
    public boolean add(Feedback f) throws Exception {
        String sql = "INSERT INTO feedback (employee_id, manager_id, eval_id, comments, rating, given_on) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getEmployeeId());
            ps.setInt(2, f.getManagerId());
            if (f.getEvalId() != null) ps.setInt(3, f.getEvalId());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, f.getComments());
            ps.setInt(5, f.getRating());
            ps.setDate(6, Date.valueOf(f.getGivenOn()));
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) f.setFeedbackId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public Feedback getById(int id) throws Exception {
        String sql = "SELECT * FROM feedback WHERE feedback_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    @Override
    public List<Feedback> getAll() throws Exception {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT * FROM feedback ORDER BY given_on DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Feedback> getByEmployee(int employeeId) throws Exception {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT * FROM feedback WHERE employee_id = ? ORDER BY given_on DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Feedback f) throws Exception {
        String sql = "UPDATE feedback SET comments=?, rating=? WHERE feedback_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getComments());
            ps.setInt(2, f.getRating());
            ps.setInt(3, f.getFeedbackId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws Exception {
        String sql = "DELETE FROM feedback WHERE feedback_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Feedback mapRow(ResultSet rs) throws SQLException {
        Feedback f = new Feedback();
        f.setFeedbackId(rs.getInt("feedback_id"));
        f.setEmployeeId(rs.getInt("employee_id"));
        f.setManagerId(rs.getInt("manager_id"));
        int evalId = rs.getInt("eval_id");
        if (!rs.wasNull()) f.setEvalId(evalId);
        f.setComments(rs.getString("comments"));
        f.setRating(rs.getInt("rating"));
        Date d = rs.getDate("given_on");
        if (d != null) f.setGivenOn(d.toLocalDate());
        return f;
    }
}
