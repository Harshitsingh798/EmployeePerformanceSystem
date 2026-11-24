import dao.DatabaseConnection;
import ui.LoginFrame;
import util.FileLogger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Performetrics - Employee Performance Evaluation System.
 *
 * <p>Entry point. It installs the FlatLaf look and feel, checks that the
 * database is reachable, and opens the login window. On exit it closes the
 * connection pool and the log file.
 */
public final class App {

    /** The application is not designed to be subclassed. */
    private App() {
        // Utility class - not instantiable.
    }

    /**
     * Starts the application.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        installLookAndFeel();

        // Close the connection pool and the log file when the JVM exits.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.getInstance().closeConnection();
            FileLogger.close();
        }, "app-shutdown"));

        if (!DatabaseConnection.isDriverAvailable()) {
            showFatal("MySQL driver missing",
                    "The MySQL JDBC driver was not found in lib/.\n"
                            + "Run the setup steps in README.md and try again.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }

    /**
     * Installs the FlatLaf look and feel, falling back to the system one.
     */
    private static void installLookAndFeel() {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default look and feel is always available, so this is safe.
            }
        }
    }

    /**
     * Shows a fatal error and stops the application.
     *
     * @param title   the dialog title
     * @param message the message to show
     */
    private static void showFatal(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
