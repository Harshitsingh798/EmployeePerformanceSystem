package util;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Loads db.properties via File I/O
 */
public class DBConfig {
    private static Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
        } catch (Exception e) {
            System.out.println("db.properties not found, using defaults");
            props.setProperty("db.url", "jdbc:mysql://localhost:3306/employee_performance_db");
            props.setProperty("db.user", "root");
            props.setProperty("db.password", "");
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
