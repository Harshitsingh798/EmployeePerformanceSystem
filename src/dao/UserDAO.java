package dao;

import model.Employee;
import model.Manager;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User DAO - handles both Manager & Employee (Polymorphism demo)
 * Demonstrates: JDBC, PreparedStatement, Collections, Exception Handling
 */
public class UserDAO implements DAO<User> {

    @Override
    public boolean add(User user) throws Exception {
        String sql = "INSERT INTO users (name, email, password, role, department, designation, manager_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole());

            if (user instanceof Manager) {
                ps.setString(5, ((Manager) user).getDepartment());
                ps.setString(6, null);
                ps.setNull(7, Types.INTEGER);
            } else if (user instanceof Employee) {
                Employee emp = (Employee) user;
                ps.setString(5, emp.getDepartment());
                ps.setString(6, emp.getDesignation());
                if (emp.getManagerId() > 0) ps.setInt(7, emp.getManagerId());
                else ps.setNull(7, Types.INTEGER);
            } else {
                ps.setString(5, null);
                ps.setString(6, null);
                ps.setNull(7, Types.INTEGER);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) user.setUserId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public User getById(int id) throws Exception {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public User getByEmail(String email) throws Exception {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /** Authentication with role check */
    public User authenticate(String email, String password, String role) throws Exception {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ? AND role = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ps.setString(3, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    @Override
    public List<User> getAll() throws Exception {
        List<User> list = new ArrayList<>(); // Collections demo
        String sql = "SELECT * FROM users ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Employee> getAllEmployees() throws Exception {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'EMPLOYEE' ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = mapRow(rs);
                if (u instanceof Employee) list.add((Employee) u);
            }
        }
        return list;
    }

    @Override
    public boolean update(User user) throws Exception {
        String sql = "UPDATE users SET name=?, email=?, password=?, department=?, designation=?, manager_id=? WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            if (user instanceof Manager) {
                ps.setString(4, ((Manager) user).getDepartment());
                ps.setString(5, null);
                ps.setNull(6, Types.INTEGER);
            } else if (user instanceof Employee) {
                Employee emp = (Employee) user;
                ps.setString(4, emp.getDepartment());
                ps.setString(5, emp.getDesignation());
                if (emp.getManagerId() > 0) ps.setInt(6, emp.getManagerId());
                else ps.setNull(6, Types.INTEGER);
            } else {
                ps.setString(4, null);
                ps.setString(5, null);
                ps.setNull(6, Types.INTEGER);
            }
            ps.setInt(7, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws Exception {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String dept = rs.getString("department");

        if ("MANAGER".equals(role)) {
            return new Manager(id, name, email, password, dept);
        } else {
            String designation = rs.getString("designation");
            int managerId = rs.getInt("manager_id");
            if (rs.wasNull()) managerId = 0;
            return new Employee(id, name, email, password, dept, designation, managerId);
        }
    }
}
