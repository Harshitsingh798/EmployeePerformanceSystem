package util;

import javax.swing.AbstractButton;
import java.awt.Color;
import java.awt.Font;

/**
 * Small helpers for styling Swing components.
 */
public final class Icons {

    /** Font family used throughout the application. */
    private static final String FONT_FAMILY = "Segoe UI";

    private Icons() {
        // Utility class - not instantiable.
    }

    /**
     * Applies a consistent look to an action button.
     *
     * @param button    the button to style
     * @param text      the label to set
     * @param background the button's background colour
     * @param foreground the label colour
     */
    public static void styleButton(AbstractButton button, String text, Color background, Color foreground) {
        button.setText(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
    }
}
