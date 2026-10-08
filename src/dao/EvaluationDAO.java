package dao;

import model.Evaluation;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluation DAO - JDBC with PreparedStatement, Collections & Generics
 */
public class EvaluationDAO implements DAO<Evaluation> {

    @Override
    public boolean add(Evaluation e) throws Exception {
        String sql = "INSERT INTO evaluations (employee_id, manager_id, communication, technical, punctuality, teamwork, overall_score, comments, eval_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getEmployeeId());
            ps.setInt(2, e.getManagerId());
            ps.setInt(3, e.getCommunication());
            ps.setInt(4, e.getTechnical());
            ps.setInt(5, e.getPunctuality());
            ps.setInt(6, e.getTeamwork());
            ps.setDouble(7, e.calculateScore());
            ps.setString(8, e.getComments());
            ps.setDate(9, Date.valueOf(e.getEvalDate()));
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) e.setEvalId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public Evaluation getById(int id) throws Exception {
        String sql = "SELECT ev.*, u.name as emp_name FROM evaluations ev LEFT JOIN users u ON ev.employee_id = u.user_id WHERE ev.eval_id = ?";
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
    public List<Evaluation> getAll() throws Exception {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT ev.*, u.name as emp_name FROM evaluations ev LEFT JOIN users u ON ev.employee_id = u.user_id ORDER BY ev.eval_date DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Evaluation> getByEmployeeId(int employeeId) throws Exception {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT ev.*, u.name as emp_name FROM evaluations ev LEFT JOIN users u ON ev.employee_id = u.user_id WHERE ev.employee_id = ? ORDER BY ev.eval_date DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Returns Map of employeeId -> List of evaluations (Collections demo) */
    public Map<Integer, List<Evaluation>> getGroupedByEmployee() throws Exception {
        Map<Integer, List<Evaluation>> map = new HashMap<>();
        for (Evaluation e : getAll()) {
            map.computeIfAbsent(e.getEmployeeId(), k -> new ArrayList<>()).add(e);
        }
        return map;
    }

    public double getAverageScore(int employeeId) throws Exception {
        String sql = "SELECT AVG(overall_score) FROM evaluations WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    @Override
    public boolean update(Evaluation e) throws Exception {
        String sql = "UPDATE evaluations SET communication=?, technical=?, punctuality=?, teamwork=?, overall_score=?, comments=? WHERE eval_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, e.getCommunication());
            ps.setInt(2, e.getTechnical());
            ps.setInt(3, e.getPunctuality());
            ps.setInt(4, e.getTeamwork());
            ps.setDouble(5, e.calculateScore());
            ps.setString(6, e.getComments());
            ps.setInt(7, e.getEvalId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws Exception {
        String sql = "DELETE FROM evaluations WHERE eval_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Evaluation mapRow(ResultSet rs) throws SQLException {
        Evaluation e = new Evaluation();
        e.setEvalId(rs.getInt("eval_id"));
        e.setEmployeeId(rs.getInt("employee_id"));
        e.setManagerId(rs.getInt("manager_id"));
        try { e.setEmployeeName(rs.getString("emp_name")); } catch (SQLException ignored) {}
        e.setCommunication(rs.getInt("communication"));
        e.setTechnical(rs.getInt("technical"));
        e.setPunctuality(rs.getInt("punctuality"));
        e.setTeamwork(rs.getInt("teamwork"));
        e.setOverallScore(rs.getDouble("overall_score"));
        e.setComments(rs.getString("comments"));
        Date d = rs.getDate("eval_date");
        if (d != null) e.setEvalDate(d.toLocalDate());
        return e;
    }
}
