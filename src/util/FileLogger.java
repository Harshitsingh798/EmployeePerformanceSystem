package util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes an audit trail to {@code app.log}.
 *
 * <p>The writer is opened once and kept open for the lifetime of the
 * application, which avoids the open/close cost of one {@code FileWriter} per
 * log line. All writes are synchronised so that the background {@code
 * SwingWorker} threads cannot interleave a line.
 */
public final class FileLogger {

    /** Name of the log file, relative to the working directory. */
    private static final String LOG_FILE = "app.log";

    /** Timestamp format used at the start of every line. */
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** The shared writer, created on first use. */
    private static Writer writer;

    private FileLogger() {
        // Utility class - not instantiable.
    }

    /**
     * Appends a message to the log file.
     *
     * @param message the message to record
     */
    public static synchronized void log(String message) {
        try {
            Writer target = writer();
            target.write("[" + LocalDateTime.now().format(TIMESTAMP) + "] " + message + System.lineSeparator());
            target.flush();
        } catch (IOException e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }

    /**
     * Records an error together with its message.
     *
     * @param message what was being attempted
     * @param error   the exception that occurred
     */
    public static void logError(String message, Exception error) {
        log("ERROR: " + message + " | " + (error == null ? "unknown error" : error.getMessage()));
    }

    /**
     * Records an informational event.
     *
     * @param action  what happened
     * @param details additional context
     */
    public static void logInfo(String action, String details) {
        log("INFO: " + action + " - " + details);
    }

    /**
     * Closes the log file. Called from the shutdown hook.
     */
    public static synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // The application is shutting down anyway.
            } finally {
                writer = null;
            }
        }
    }

    /**
     * Opens the log file on first use.
     */
    private static Writer writer() throws IOException {
        if (writer == null) {
            writer = new BufferedWriter(Files.newBufferedWriter(
                    Path.of(LOG_FILE),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND));
            Runtime.getRuntime().addShutdownHook(new Thread(FileLogger::close, "file-logger-shutdown"));
        }
        return writer;
    }
}
