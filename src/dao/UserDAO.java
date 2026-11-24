package dao;

import model.Employee;
import model.Manager;
import model.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the {@code users} table, covering both managers and
 * employees.
 *
 * <p>Rows are mapped onto the polymorphic {@link User} hierarchy, so callers
 * receive a {@link Manager} or an {@link Employee} instance depending on the
 * {@code role} column.
 */
public class UserDAO implements DAO<User> {

    /** Explicit column list - avoids {@code SELECT *} surprises when the schema changes. */
    private static final String COLUMNS =
            "user_id, name, email, password, role, department, designation, manager_id";

    @Override
    public boolean add(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, role, department, designation, manager_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole());

            if (user instanceof Manager manager) {
                ps.setString(5, manager.getDepartment());
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.INTEGER);
            } else if (user instanceof Employee employee) {
                ps.setString(5, employee.getDepartment());
                ps.setString(6, employee.getDesignation());
                if (employee.getManagerId() > 0) {
                    ps.setInt(7, employee.getManagerId());
                } else {
                    ps.setNull(7, Types.INTEGER);
                }
            } else {
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.INTEGER);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setUserId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public User getById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";
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
     * Loads a user by e-mail address.
     *
     * @param email the e-mail address
     * @return the user, or {@code null} when no such row exists
     * @throws SQLException if the query fails
     */
    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Authenticates a user and transparently upgrades legacy password hashes.
     *
     * <p>When the stored value is still a plain-text or SHA-256 hash it is
     * replaced with a PBKDF2 hash as part of the successful login, so accounts
     * migrate to the strong format without an administrator having to reset
     * passwords.
     *
     * @param email    the e-mail address typed by the user
     * @param password the plain-text password typed by the user
     * @param role     the role the user is trying to sign in as
     * @return the authenticated user, or {@code null} when the credentials do not match
     * @throws SQLException if the query fails
     */
    public User authenticate(String email, String password, String role) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE email = ? AND role = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = mapRow(rs);
                if (!PasswordUtil.verify(password, user.getPassword())) {
                    return null;
                }
                if (PasswordUtil.needsRehash(user.getPassword())) {
                    String upgraded = PasswordUtil.hash(password);
                    if (updatePassword(user.getUserId(), upgraded)) {
                        user.setPassword(upgraded);
                    }
                }
                return user;
            }
        }
    }

    @Override
    public List<User> getAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY user_id";
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
     * Loads every employee in the database.
     *
     * @return the employees
     * @throws SQLException if the query fails
     */
    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM users WHERE role = 'EMPLOYEE' ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = mapRow(rs);
                if (user instanceof Employee employee) {
                    list.add(employee);
                }
            }
        }
        return list;
    }

    /**
     * Loads the employees reporting to a given manager.
     *
     * <p>This is the scoping query the manager dashboard uses so that a
     * manager only ever sees their own team.
     *
     * @param managerId the manager's user id
     * @return the direct reports of that manager
     * @throws SQLException if the query fails
     */
    public List<Employee> getEmployeesForManager(int managerId) throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM users WHERE role = 'EMPLOYEE' AND manager_id = ? ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = mapRow(rs);
                    if (user instanceof Employee employee) {
                        list.add(employee);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET name=?, email=?, password=?, department=?, designation=?, manager_id=? "
                + "WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            if (user instanceof Manager manager) {
                ps.setString(4, manager.getDepartment());
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.INTEGER);
            } else if (user instanceof Employee employee) {
                ps.setString(4, employee.getDepartment());
                ps.setString(5, employee.getDesignation());
                if (employee.getManagerId() > 0) {
                    ps.setInt(6, employee.getManagerId());
                } else {
                    ps.setNull(6, Types.INTEGER);
                }
            } else {
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.INTEGER);
            }
            ps.setInt(7, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Replaces the stored password hash of a user.
     *
     * @param userId     the user's primary key
     * @param passwordHash the already-hashed password
     * @return {@code true} when the row was updated
     * @throws SQLException if the update fails
     */
    public boolean updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password=? WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row onto the {@link User} hierarchy.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String department = rs.getString("department");

        if ("MANAGER".equals(role)) {
            return new Manager(id, name, email, password, department);
        }
        String designation = rs.getString("designation");
        int managerId = rs.getInt("manager_id");
        if (rs.wasNull()) {
            managerId = 0;
        }
        return new Employee(id, name, email, password, department, designation, managerId);
    }
}
