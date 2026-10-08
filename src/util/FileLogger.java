package util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * File I/O demo - logs events to file (Thread-safe via synchronized)
 */
public class FileLogger {

    private static final String LOG_FILE = "app.log";
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write("[" + LocalDateTime.now().format(fmt) + "] " + message + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }

    public static void logError(String message, Exception e) {
        log("ERROR: " + message + " | " + e.getMessage());
    }

    public static void logInfo(String action, String details) {
        log("INFO: " + action + " - " + details);
    }
}
