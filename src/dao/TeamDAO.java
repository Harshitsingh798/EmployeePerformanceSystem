package dao;

import model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the {@code teams} and {@code team_members} tables.
 */
public class TeamDAO implements DAO<Team> {

    /** Explicit column list. */
    private static final String COLUMNS = "team_id, team_name, manager_id, created_at";

    @Override
    public boolean add(Team t) throws SQLException {
        String sql = "INSERT INTO teams (team_name, manager_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getTeamName());
            ps.setInt(2, t.getManagerId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        t.setTeamId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Adds a member to a team.
     *
     * @param teamId     the team's id
     * @param employeeId the employee's user id
     * @param role       the role the employee holds in the team
     * @return {@code true} when the membership was created
     * @throws SQLException if the insert fails
     */
    public boolean addMember(int teamId, int employeeId, String role) throws SQLException {
        String sql = "INSERT INTO team_members (team_id, employee_id, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, employeeId);
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Removes a member from a team.
     *
     * @param teamId     the team's id
     * @param employeeId the employee's user id
     * @return {@code true} when the membership was removed
     * @throws SQLException if the delete fails
     */
    public boolean removeMember(int teamId, int employeeId) throws SQLException {
        String sql = "DELETE FROM team_members WHERE team_id=? AND employee_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Changes the role a member holds in a team.
     *
     * <p>This is a single UPDATE rather than a remove-then-add pair, so the
     * membership can never be lost if the second statement fails.
     *
     * @param teamId     the team's id
     * @param employeeId the employee's user id
     * @param role       the new role
     * @return {@code true} when a row was changed
     * @throws SQLException if the update fails
     */
    public boolean updateMemberRole(int teamId, int employeeId, String role) throws SQLException {
        String sql = "UPDATE team_members SET role=? WHERE team_id=? AND employee_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, teamId);
            ps.setInt(3, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lists the members of a team together with their role.
     *
     * @param teamId the team's id
     * @return one entry per member; each entry is {@code [userId, name, role]}
     * @throws SQLException if the query fails
     */
    public List<Object[]> getMembers(int teamId) throws SQLException {
        List<Object[]> members = new ArrayList<>();
        String sql = "SELECT tm.employee_id, u.name, tm.role FROM team_members tm "
                + "JOIN users u ON tm.employee_id = u.user_id WHERE tm.team_id = ? ORDER BY u.name";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
                }
            }
        }
        return members;
    }

    @Override
    public Team getById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM teams WHERE team_id = ?";
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
    public List<Team> getAll() throws SQLException {
        List<Team> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM teams ORDER BY team_id";
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
     * Loads the teams managed by one manager.
     *
     * @param managerId the manager's user id
     * @return the teams
     * @throws SQLException if the query fails
     */
    public List<Team> getByManager(int managerId) throws SQLException {
        List<Team> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM teams WHERE manager_id = ? ORDER BY team_id";
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
    public boolean update(Team t) throws SQLException {
        String sql = "UPDATE teams SET team_name=? WHERE team_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTeamName());
            ps.setInt(2, t.getTeamId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM teams WHERE team_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row onto a {@link Team}.
     */
    private Team mapRow(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setTeamId(rs.getInt("team_id"));
        team.setTeamName(rs.getString("team_name"));
        team.setManagerId(rs.getInt("manager_id"));
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            team.setCreatedAt(createdAt.toLocalDateTime());
        }
        return team;
    }
}
