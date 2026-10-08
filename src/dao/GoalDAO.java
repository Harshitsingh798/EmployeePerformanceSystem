package dao;

import model.Goal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Goal DAO - covers both Manager goals and Personal goals
 */
public class GoalDAO implements DAO<Goal> {

    @Override
    public boolean add(Goal g) throws Exception {
        String sql = "INSERT INTO goals (title, description, assigned_to, assigned_by, target_date, status, is_personal) VALUES (?, ?, ?, ?, ?, ?, ?)";
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
                    if (rs.next()) g.setGoalId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public Goal getById(int id) throws Exception {
        String sql = "SELECT * FROM goals WHERE goal_id = ?";
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
    public List<Goal> getAll() throws Exception {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals ORDER BY target_date ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Goal> getByEmployee(int employeeId) throws Exception {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE assigned_to = ? ORDER BY target_date ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Goal> getPersonalGoals(int employeeId) throws Exception {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE assigned_to = ? AND is_personal = TRUE ORDER BY target_date ASC";
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
    public boolean update(Goal g) throws Exception {
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

    public boolean updateStatus(int goalId, String status) throws Exception {
        String sql = "UPDATE goals SET status=? WHERE goal_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, goalId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws Exception {
        String sql = "DELETE FROM goals WHERE goal_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Goal mapRow(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setGoalId(rs.getInt("goal_id"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        g.setAssignedTo(rs.getInt("assigned_to"));
        g.setAssignedBy(rs.getInt("assigned_by"));
        Date d = rs.getDate("target_date");
        if (d != null) g.setTargetDate(d.toLocalDate());
        g.setStatus(rs.getString("status"));
        g.setPersonal(rs.getBoolean("is_personal"));
        try {
            Date c = rs.getDate("created_at");
            if (c != null) g.setCreatedAt(c.toLocalDate());
        } catch (SQLException ignored) {}
        return g;
    }
}
