import ui.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Performetrics - Employee Performance Evaluation System
 * Entry Point
 */
public class App {
    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        }

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
