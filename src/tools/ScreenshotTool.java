package tools;

import ui.LoginFrame;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;

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
     * Switches a window to the tab with the given title.
     *
     * @param frame    the window to look in
     * @param tabTitle the title of the tab to select
     */
    private static void selectTab(java.awt.Container frame, String tabTitle) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.JTabbedPane tabs = findTabbedPane(frame);
            if (tabs == null) {
                System.out.println("No tabbed pane found for " + tabTitle);
                return;
            }
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (tabTitle.equals(tabs.getTitleAt(i))) {
                    tabs.setSelectedIndex(i);
                    System.out.println("Selected tab: " + tabTitle);
                    return;
                }
            }
            System.out.println("No tab named " + tabTitle);
        });
    }

    /**
     * Finds the first tabbed pane inside a container.
     *
     * @param container the container to search
     * @return the tabbed pane, or {@code null} when there is none
     */
    private static javax.swing.JTabbedPane findTabbedPane(java.awt.Container container) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof javax.swing.JTabbedPane tabs) {
                return tabs;
            }
            if (child instanceof java.awt.Container nested) {
                javax.swing.JTabbedPane found = findTabbedPane(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
