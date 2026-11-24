package dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton entry point for database access.
 *
 * <p>The class owns the connection {@link ConnectionPool} and the configuration
 * loaded from {@code db.properties}. It is a singleton so that every DAO in the
 * application shares one pool - creating a pool per DAO would defeat the
 * purpose.
 *
 * <p>Configuration lookup order:
 * <ol>
 *   <li>the {@code db.properties} file in the working directory,</li>
 *   <li>the {@code db.properties} resource on the classpath,</li>
 *   <li>built-in defaults (local MySQL, {@code root}, empty password).</li>
 * </ol>
 * Because a password may be present in that file, {@code db.properties} is
 * excluded from version control - see {@code db.properties.example}.
 */
public final class DatabaseConnection {

    /** Name of the configuration file, looked up on disk and on the classpath. */
    private static final String CONFIG_FILE = "db.properties";

    /** Default JDBC URL used when no configuration file is found. */
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/employee_performance_db";

    /** Default database user used when no configuration file is found. */
    private static final String DEFAULT_USER = "root";

    /** Default database password used when no configuration file is found. */
    private static final String DEFAULT_PASSWORD = "";

    /** The single instance - guarded by {@link #getInstance()}. */
    private static DatabaseConnection instance;

    /** The pool that hands out connections to the DAOs. */
    private final ConnectionPool pool;

    private String url;
    private String user;
    private String password;

    /**
     * Creates the pool and loads the configuration. Kept private so that the
     * class can only be instantiated through {@link #getInstance()}.
     */
    private DatabaseConnection() {
        loadConfig();
        loadDriver();
        this.pool = new ConnectionPool(url, user, password, ConnectionPool.DEFAULT_MAX_SIZE);
    }

    /**
     * Returns the shared instance, creating it on first use.
     *
     * <p>Synchronized so that two threads racing to create the instance cannot
     * end up with two different pools.
     *
     * @return the singleton instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Borrows a connection from the pool.
     *
     * <p>The returned connection must be closed by the caller; closing it
     * returns it to the pool instead of tearing down the socket. Using it in a
     * try-with-resources block is the intended usage:
     *
     * <pre>{@code
     * try (Connection conn = DatabaseConnection.getInstance().getConnection();
     *      PreparedStatement ps = conn.prepareStatement(sql)) {
     *     // ...
     * }
     * }</pre>
     *
     * @return a pooled connection
     * @throws SQLException if no connection could be obtained
     */
    public Connection getConnection() throws SQLException {
        return pool.borrow();
    }

    /**
     * Closes the pool and every connection it owns. Called on application
     * shutdown.
     */
    public void closeConnection() {
        pool.close();
    }

    /**
     * Verifies that the database is reachable and that credentials work.
     *
     * @return {@code true} when a connection can be established
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            System.err.println("DB connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * @return the JDBC URL currently in use
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the database user currently in use
     */
    public String getUser() {
        return user;
    }

    /**
     * Loads {@code db.properties} from the working directory first and then
     * from the classpath.
     */
    private void loadConfig() {
        Properties properties = new Properties();
        boolean loaded = false;

        java.io.File onDisk = new java.io.File(CONFIG_FILE);
        if (onDisk.isFile()) {
            try (InputStream input = new java.io.FileInputStream(onDisk)) {
                properties.load(input);
                loaded = true;
            } catch (IOException e) {
                System.err.println("Could not read " + CONFIG_FILE + ": " + e.getMessage());
            }
        }

        if (!loaded) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (input != null) {
                    properties.load(input);
                    loaded = true;
                }
            } catch (IOException e) {
                System.err.println("Could not read " + CONFIG_FILE + " from the classpath: " + e.getMessage());
            }
        }

        url = properties.getProperty("db.url", DEFAULT_URL).trim();
        user = properties.getProperty("db.user", DEFAULT_USER).trim();
        password = properties.getProperty("db.password", DEFAULT_PASSWORD);
        if (!loaded) {
            System.out.println(CONFIG_FILE + " not found - using defaults for " + url);
        }
    }

    /**
     * Ensures the MySQL driver is registered. Modern drivers auto-register via
     * the service loader, but the explicit load keeps the behaviour identical
     * on every JVM.
     */
    private void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver not found. Add mysql-connector-j to lib/ - " + e.getMessage());
        }
    }

    /**
     * @return {@code true} when the MySQL driver class is on the classpath
     */
    public static boolean isDriverAvailable() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * @return the underlying pool, mainly for diagnostics and tests
     */
    public ConnectionPool getPool() {
        return pool;
    }

    /**
     * @return {@code true} when the given URL can be parsed by the driver
     */
    static boolean isValidJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return false;
        }
        try {
            DriverManager.getDriver(jdbcUrl);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
