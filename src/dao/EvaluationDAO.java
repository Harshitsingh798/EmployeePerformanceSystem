package dao;

import model.Evaluation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access for the {@code evaluations} table.
 */
public class EvaluationDAO implements DAO<Evaluation> {

    /** Column list shared by every query, including the joined employee name. */
    private static final String SELECT =
            "SELECT ev.eval_id, ev.employee_id, ev.manager_id, ev.communication, ev.technical, "
                    + "ev.punctuality, ev.teamwork, ev.overall_score, ev.comments, ev.eval_date, u.name AS emp_name "
                    + "FROM evaluations ev LEFT JOIN users u ON ev.employee_id = u.user_id";

    @Override
    public boolean add(Evaluation e) throws SQLException {
        String sql = "INSERT INTO evaluations (employee_id, manager_id, communication, technical, punctuality, "
                + "teamwork, overall_score, comments, eval_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                    if (rs.next()) {
                        e.setEvalId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public Evaluation getById(int id) throws SQLException {
        String sql = SELECT + " WHERE ev.eval_id = ?";
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
    public List<Evaluation> getAll() throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String sql = SELECT + " ORDER BY ev.eval_date DESC, ev.eval_id DESC";
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
     * Loads every evaluation for one employee.
     *
     * @param employeeId the employee's user id
     * @return the evaluations, newest first
     * @throws SQLException if the query fails
     */
    public List<Evaluation> getByEmployeeId(int employeeId) throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String sql = SELECT + " WHERE ev.employee_id = ? ORDER BY ev.eval_date DESC, ev.eval_id DESC";
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
     * Loads every evaluation created by one manager.
     *
     * <p>This keeps a manager inside their own team in the manager dashboard.
     *
     * @param managerId the manager's user id
     * @return the evaluations, newest first
     * @throws SQLException if the query fails
     */
    public List<Evaluation> getByManagerId(int managerId) throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String sql = SELECT + " WHERE ev.manager_id = ? ORDER BY ev.eval_date DESC, ev.eval_id DESC";
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

    /**
     * Searches evaluations by employee name or comment text.
     *
     * <p>The filtering happens in the database so that large tables are not
     * shipped to the client just to be discarded.
     *
     * @param text  the search term; a blank term returns every evaluation
     * @param scope when non-zero, only evaluations created by that manager are considered
     * @return the matching evaluations
     * @throws SQLException if the query fails
     */
    public List<Evaluation> search(String text, int scope) throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        boolean hasText = text != null && !text.isBlank();
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        if (scope > 0) {
            sql.append(" AND ev.manager_id = ?");
        }
        if (hasText) {
            sql.append(" AND (LOWER(u.name) LIKE ? OR LOWER(ev.comments) LIKE ?)");
        }
        sql.append(" ORDER BY ev.eval_date DESC, ev.eval_id DESC");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            if (scope > 0) {
                ps.setInt(index++, scope);
            }
            if (hasText) {
                String pattern = "%" + text.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                ps.setString(index++, pattern);
                ps.setString(index, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Groups every evaluation by employee id.
     *
     * @return a map of employee id to that employee's evaluations
     * @throws SQLException if the query fails
     */
    public Map<Integer, List<Evaluation>> getGroupedByEmployee() throws SQLException {
        Map<Integer, List<Evaluation>> map = new HashMap<>();
        for (Evaluation evaluation : getAll()) {
            map.computeIfAbsent(evaluation.getEmployeeId(), key -> new ArrayList<>()).add(evaluation);
        }
        return map;
    }

    /**
     * Computes the average overall score of one employee in the database.
     *
     * @param employeeId the employee's user id
     * @return the average score, or {@code 0.0} when there are no evaluations
     * @throws SQLException if the query fails
     */
    public double getAverageScore(int employeeId) throws SQLException {
        String sql = "SELECT AVG(overall_score) FROM evaluations WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double average = rs.getDouble(1);
                    return rs.wasNull() ? 0.0 : average;
                }
            }
        }
        return 0.0;
    }

    /**
     * Loads the average overall score per employee for a set of employees.
     *
     * <p>The aggregation is done by MySQL instead of in Java so that only one
     * small row per employee travels over the wire.
     *
     * @param scope when non-zero, only evaluations created by that manager are aggregated
     * @return a map of employee id to average score
     * @throws SQLException if the query fails
     */
    public Map<Integer, Double> getAverageScoresByEmployee(int scope) throws SQLException {
        Map<Integer, Double> result = new HashMap<>();
        StringBuilder sql = new StringBuilder(
                "SELECT ev.employee_id, AVG(ev.overall_score) FROM evaluations ev WHERE 1 = 1");
        if (scope > 0) {
            sql.append(" AND ev.manager_id = ?");
        }
        sql.append(" GROUP BY ev.employee_id");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (scope > 0) {
                ps.setInt(1, scope);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double average = rs.getDouble(2);
                    result.put(rs.getInt(1), rs.wasNull() ? 0.0 : average);
                }
            }
        }
        return result;
    }

    @Override
    public boolean update(Evaluation e) throws SQLException {
        String sql = "UPDATE evaluations SET communication=?, technical=?, punctuality=?, teamwork=?, "
                + "overall_score=?, comments=? WHERE eval_id=?";
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

    /**
     * Deletes an evaluation only when it belongs to the given manager.
     *
     * @param id        the evaluation id
     * @param managerId the manager attempting the delete
     * @return {@code true} when the evaluation was removed
     * @throws SQLException if the delete fails
     */
    public boolean deleteByManager(int id, int managerId) throws SQLException {
        String sql = "DELETE FROM evaluations WHERE eval_id = ? AND manager_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, managerId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM evaluations WHERE eval_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row onto an {@link Evaluation}.
     */
    private Evaluation mapRow(ResultSet rs) throws SQLException {
        Evaluation evaluation = new Evaluation();
        evaluation.setEvalId(rs.getInt("eval_id"));
        evaluation.setEmployeeId(rs.getInt("employee_id"));
        evaluation.setManagerId(rs.getInt("manager_id"));
        evaluation.setEmployeeName(rs.getString("emp_name"));
        evaluation.setCommunication(rs.getInt("communication"));
        evaluation.setTechnical(rs.getInt("technical"));
        evaluation.setPunctuality(rs.getInt("punctuality"));
        evaluation.setTeamwork(rs.getInt("teamwork"));
        evaluation.setComments(rs.getString("comments"));
        Date date = rs.getDate("eval_date");
        if (date != null) {
            evaluation.setEvalDate(date.toLocalDate());
        }
        return evaluation;
    }
}
