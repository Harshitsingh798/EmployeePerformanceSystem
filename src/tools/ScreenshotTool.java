package tools;

import ui.LoginFrame;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import ui.components.Sidebar;

/**
 * Captures screenshots of the running application for the documentation.
 *
 * <p>Run it with the project root as the working directory:
 *
 * <pre>{@code
 * java -cp "bin;lib/*" tools.ScreenshotTool
 * }</pre>
 *
 * <p>Each screenshot is taken of the application's own window rather than of
 * the whole screen, so whatever else is open on the desktop cannot end up in
 * the picture.
 *
 * <p>Both dashboards now use a {@link CardLayout}-based workspace driven by the
 * {@link Sidebar} navigation, so {@link #selectTab} finds the sidebar and clicks
 * the nav button whose label matches the requested tab title.
 */
public final class ScreenshotTool {

    /** Where the images are written. */
    private static final File OUTPUT_DIR = new File("docs", "screenshots");

    /** How long to wait for a window to load its data before capturing it. */
    private static final long SETTLE_MILLIS = 4000L;

    /** How long to wait after raising a window before capturing it. */
    private static final long RAISE_MILLIS = 700L;

    private ScreenshotTool() {
        // Utility class - not instantiable.
    }

    /**
     * Entry point.
     *
     * @param args ignored
     */
    public static void main(String[] args) throws Exception {
        if (!OUTPUT_DIR.exists() && !OUTPUT_DIR.mkdirs()) {
            throw new IllegalStateException("Could not create " + OUTPUT_DIR.getAbsolutePath());
        }

        // The login window.
        JFrame login = new LoginFrame();
        SwingUtilities.invokeAndWait(() -> {
            com.formdev.flatlaf.FlatLightLaf.setup();
            login.setVisible(true);
        });
        Thread.sleep(SETTLE_MILLIS);
        capture(login, "01-login.png");
        login.dispose();

        // The manager dashboard.
        ui.ManagerDashboard manager = new ui.ManagerDashboard("Rajesh Kumar", 1);
        SwingUtilities.invokeAndWait(() -> manager.setVisible(true));
        Thread.sleep(SETTLE_MILLIS);
        capture(manager, "02-manager-dashboard.png");

        // The manager's reports tab, with the criteria charts.
        selectTab(manager, "Performance Reports");
        Thread.sleep(SETTLE_MILLIS);
        capture(manager, "03-manager-reports.png");
        manager.dispose();

        // The employee dashboard.
        ui.EmployeeDashboard employee = new ui.EmployeeDashboard("Priya Sharma", 2);
        SwingUtilities.invokeAndWait(() -> employee.setVisible(true));
        Thread.sleep(SETTLE_MILLIS);
        capture(employee, "04-employee-dashboard.png");

        // The employee's reports tab, with the criteria charts.
        selectTab(employee, "Performance Reports");
        Thread.sleep(SETTLE_MILLIS);
        capture(employee, "05-employee-reports.png");
        employee.dispose();

        System.out.println("Screenshots written to " + OUTPUT_DIR.getAbsolutePath());
        System.exit(0);
    }

    /**
     * Switches a window to the card with the given sidebar label.
     *
     * @param frame     the window to look in
     * @param tabTitle  the label of the sidebar button to click
     */
    private static void selectTab(java.awt.Container frame, String tabTitle) {
        SwingUtilities.invokeLater(() -> {
            Sidebar sidebar = findSidebar(frame);
            if (sidebar == null) {
                System.out.println("No sidebar found for " + tabTitle);
                return;
            }
            boolean found = clickButtonByText(sidebar, tabTitle);
            if (found) {
                System.out.println("Selected tab: " + tabTitle);
            } else {
                System.out.println("No tab named " + tabTitle);
            }
        });
    }

    /**
     * Finds the {@link Sidebar} inside a window, searching recursively.
     *
     * @param container the container to search
     * @return the sidebar, or {@code null} when there is none
     */
    private static Sidebar findSidebar(java.awt.Container container) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof Sidebar sidebar) {
                return sidebar;
            }
            if (child instanceof java.awt.Container nested) {
                Sidebar found = findSidebar(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Clicks the first button inside the sidebar whose label matches the given
     * title (comparison is case-insensitive and trims whitespace).
     *
     * @param sidebar the sidebar to search
     * @param title   the button label to match
     * @return {@code true} when a matching button was clicked
     */
    private static boolean clickButtonByText(Sidebar sidebar, String title) {
        return clickButtonByTextRecursive(sidebar, title);
    }

    private static boolean clickButtonByTextRecursive(java.awt.Container container, String title) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof AbstractButton button) {
                if (title != null && title.trim().equalsIgnoreCase(button.getText().trim())) {
                    button.doClick();
                    return true;
                }
            }
            if (child instanceof java.awt.Container nested) {
                if (clickButtonByTextRecursive(nested, title)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Captures one application window.
     *
     * <p>The window is raised above everything else and pinned to the top for
     * the moment of the capture, so the region of the screen it occupies
     * really does show the window rather than whatever was behind it.
     *
     * @param window   the window to photograph
     * @param fileName the name of the image file
     * @throws Exception if the screen cannot be captured
     */
    private static void capture(Window window, String fileName) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Park the window in the top-left corner, well inside the screen,
            // so the captured region cannot run off the edge and pick up the
            // desktop, the taskbar or whatever is behind it.
            window.setLocation(40, 40);
            window.setAlwaysOnTop(true);
            window.toFront();
            window.requestFocus();
        });
        Thread.sleep(RAISE_MILLIS);

        Rectangle bounds = window.getBounds();
        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(bounds);

        SwingUtilities.invokeAndWait(() -> window.setAlwaysOnTop(false));

        File target = new File(OUTPUT_DIR, fileName);
        ImageIO.write(image, "png", target);
        System.out.println("Captured " + target.getAbsolutePath() + "  (" + bounds.width + "x"
                + bounds.height + ")");
    }
}
