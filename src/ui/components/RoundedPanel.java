package ui.components;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * A card surface with a custom anti-aliased fill and a subtle 1px border.
 *
 * <p>Used throughout the Performetrics dashboards so every KPI card, table
 * wrapper and chart card shares the same rounded white look on the
 * {@code #F4F6FA} canvas.
 */
public class RoundedPanel extends JPanel {

    /** Serialisation version - required for a {@link java.io.Serializable} subclass. */
    private static final long serialVersionUID = 1L;

    /** Corner radius in pixels. */
    private final int radius;

    /** Border colour, or {@code null} to paint no border. */
    private final Color borderColor;

    /** Border stroke width in pixels. */
    private final float borderWidth;

    /**
     * Creates a white rounded card with a subtle border.
     *
     * @param radius the corner radius in pixels
     */
    public RoundedPanel(int radius) {
        this(radius, new Color(0xE2E8F0), 1f);
    }

    /**
     * Creates a rounded card.
     *
     * @param radius      the corner radius in pixels
     * @param borderColor the border colour
     * @param borderWidth the border stroke width
     */
    @SuppressWarnings("this-escape")
    public RoundedPanel(int radius, Color borderColor, float borderWidth) {
        this.radius = radius;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
        setOpaque(false);
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(16, 16, 16, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth();
            int h = getHeight();

            // Card fill.
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, radius, radius);

            // Subtle border.
            if (borderColor != null && borderWidth > 0) {
                g2.setColor(borderColor);
                g2.setStroke(new java.awt.BasicStroke(borderWidth));
                g2.drawRoundRect(
                        (int) (borderWidth / 2f), (int) (borderWidth / 2f),
                        w - (int) Math.ceil(borderWidth), h - (int) Math.ceil(borderWidth),
                        radius, radius);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    @Override
    public Insets getInsets() {
        Insets base = super.getInsets();
        int pad = borderWidth > 0 ? (int) Math.ceil(borderWidth) + 1 : 0;
        return new Insets(base.top + pad, base.left + pad, base.bottom + pad, base.right + pad);
    }
}
