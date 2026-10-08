package dao;

import model.Team;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Team DAO - handles teams and team_members
 */
public class TeamDAO implements DAO<Team> {

    @Override
    public boolean add(Team t) throws Exception {
        String sql = "INSERT INTO teams (team_name, manager_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTeamName());
            ps.setInt(2, t.getManagerId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) t.setTeamId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    public boolean addMember(int teamId, int employeeId, String role) throws Exception {
        String sql = "INSERT INTO team_members (team_id, employee_id, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, employeeId);
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeMember(int teamId, int employeeId) throws Exception {
        String sql = "DELETE FROM team_members WHERE team_id=? AND employee_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Team getById(int id) throws Exception {
        String sql = "SELECT * FROM teams WHERE team_id = ?";
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
    public List<Team> getAll() throws Exception {
        List<Team> list = new ArrayList<>();
        String sql = "SELECT * FROM teams ORDER BY team_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Team> getByManager(int managerId) throws Exception {
        List<Team> list = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE manager_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Team t) throws Exception {
        String sql = "UPDATE teams SET team_name=? WHERE team_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTeamName());
            ps.setInt(2, t.getTeamId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws Exception {
        String sql = "DELETE FROM teams WHERE team_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Team mapRow(ResultSet rs) throws SQLException {
        Team t = new Team();
        t.setTeamId(rs.getInt("team_id"));
        t.setTeamName(rs.getString("team_name"));
        t.setManagerId(rs.getInt("manager_id"));
        return t;
    }
}
