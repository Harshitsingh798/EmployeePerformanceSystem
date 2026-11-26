import dao.DatabaseConnection;
import ui.LoginFrame;
import util.FileLogger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

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

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /**
     * Installs the FlatLaf look and feel with the Performetrics design system
     * applied, falling back to the system one.
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

        applyDesignSystem();
    }

    /**
     * Applies the Performetrics light SaaS design system on top of FlatLaf:
     * rounded corners, clean typography, subtle focus borders, light table and
     * dialog styling, and a quiet scrollbar look.
     */
    private static void applyDesignSystem() {
        // ---------- Color palette ----------
        Color sidebarBg = new Color(0x0F172A);      // dark navy
        Color sidebarCard = new Color(0x1E293B);    // sidebar profile/hover
        Color activePill = new Color(0x4F46E5);     // indigo active nav item
        Color canvasBg = new Color(0xF4F6FA);       // main content canvas
        Color cardBg = Color.WHITE;                 // card surface
        Color border = new Color(0xE2E8F0);         // subtle 1px border
        Color mutedText = new Color(0x64748B);      // muted labels
        Color bodyText = new Color(0x334155);       // body text
        Color headingText = new Color(0x0F172A);    // headings

        // ---------- Global FlatLaf defaults ----------
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", new Color(0x4F46E5));
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Table.arc", 10);
        UIManager.put("ScrollBar.arc", 10);
        UIManager.put("ScrollBar.thumb.arc", 10);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.trackInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("PopupMenu.arc", 10);
        UIManager.put("OptionPane.arc", 12);
        UIManager.put("ProgressBar.arc", 8);

        // ---------- Typography ----------
        UIManager.put("Font", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        UIManager.put("Button.font", new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        UIManager.put("Label.font", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        UIManager.put("Table.font", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        UIManager.put("ComboBox.font", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));

        // ---------- Tables ----------
        UIManager.put("Table.rowHeight", 44);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 0));
        UIManager.put("Table.gridColor", new Color(0xF1F5F9));
        UIManager.put("Table.selectionBackground", activePill);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.headerBackground", cardBg);
        UIManager.put("Table.headerForeground", mutedText);
        UIManager.put("Table.cellBackground", cardBg);

        // ---------- Scrollbars (quiet, light) ----------
        UIManager.put("ScrollBar.track", new Color(0xF1F5F9));
        UIManager.put("ScrollBar.thumb", new Color(0xCBD5E1));
        UIManager.put("ScrollBar.thumbHover", new Color(0x94A3B8));

        // ---------- Buttons ----------
        UIManager.put("Button.defaultButtonFollowsFocus", false);

        // Store palette keys for components to reference.
        UIManager.put("Performetrics.sidebarBg", sidebarBg);
        UIManager.put("Performetrics.sidebarCard", sidebarCard);
        UIManager.put("Performetrics.activePill", activePill);
        UIManager.put("Performetrics.canvasBg", canvasBg);
        UIManager.put("Performetrics.cardBg", cardBg);
        UIManager.put("Performetrics.border", border);
        UIManager.put("Performetrics.mutedText", mutedText);
        UIManager.put("Performetrics.bodyText", bodyText);
        UIManager.put("Performetrics.headingText", headingText);
        UIManager.put("Performetrics.primary", activePill);
        UIManager.put("Performetrics.primaryHover", new Color(0x4338CA));
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
