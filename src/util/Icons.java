package util;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Font;

public class Icons {
    public static ImageIcon getIcon(String name, int size) {
        try {
            java.net.URL url = Icons.class.getResource("/icons/" + name + ".png");
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                if (size > 0) {
                    java.awt.Image img = icon.getImage().getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                }
                return icon;
            }
        } catch (Exception e) {
            // Fallback to text
        }
        return null;
    }

    public static void styleButton(javax.swing.AbstractButton btn, String text, Color bg, Color fg) {
        btn.setText(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }
}
