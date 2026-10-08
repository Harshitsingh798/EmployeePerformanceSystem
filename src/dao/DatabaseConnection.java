package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton Database Connection - demonstrates Multithreading & Synchronization
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    // Default config (fallback if db.properties not found)
    private String url = "jdbc:mysql://localhost:3306/employee_performance_db";
    private String user = "root";
    private String password = "";

    private DatabaseConnection() {
        loadConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL Driver not found. Add mysql-connector-j to lib/");
        }
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                url = prop.getProperty("db.url", url);
                user = prop.getProperty("db.user", user);
                password = prop.getProperty("db.password", password);
            }
        } catch (Exception e) {
            System.out.println("db.properties not found, using defaults: " + e.getMessage());
        }
    }

    // Synchronized for thread safety
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected: " + url);
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Test connection
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("DB Connection failed: " + e.getMessage());
            return false;
        }
    }
}
