package util;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Small helpers and crisp vector icons for styling Swing components according
 * to the Performetrics light SaaS design system.
 *
 * <p>FlatLaf {@link javax.swing.UIManager} defaults are installed in {@code App};
 * these helpers apply per-component colours, padding, hover behaviour and
 * anti-aliased {@link Icon}s so the dashboards, login window and dialogs all
 * share one clean look.
 */
public final class Icons {

    /** Font family used throughout the application. */
    public static final String FONT_FAMILY = "Segoe UI";

    /** Primary indigo (#4F46E5). */
    public static final Color PRIMARY = new Color(0x4F46E5);
    /** Primary indigo hover (#4338CA). */
    public static final Color PRIMARY_HOVER = new Color(0x4338CA);
    /** Soft indigo tint (#EEF2FF). */
    public static final Color PRIMARY_TINT = new Color(0xEEF2FF);
    /** Sidebar dark navy (#0F172A). */
    public static final Color SIDEBAR_BG = new Color(0x0F172A);
    /** Sidebar card/hover (#1E293B). */
    public static final Color SIDEBAR_CARD = new Color(0x1E293B);
    /** Sidebar border (#334155). */
    public static final Color SIDEBAR_BORDER = new Color(0x334155);
    /** Active nav pill (#4F46E5). */
    public static final Color ACTIVE_PILL = new Color(0x4F46E5);
    /** Main content canvas (#F4F6FA). */
    public static final Color CANVAS_BG = new Color(0xF4F6FA);
    /** Card surface (#FFFFFF). */
    public static final Color CARD_BG = Color.WHITE;
    /** Subtle 1px border (#E2E8F0). */
    public static final Color BORDER = new Color(0xE2E8F0);
    /** Row divider (#F1F5F9). */
    public static final Color ROW_DIVIDER = new Color(0xF1F5F9);
    /** Muted label (#64748B). */
    public static final Color MUTED_TEXT = new Color(0x64748B);
    /** Body text (#334155). */
    public static final Color BODY_TEXT = new Color(0x334155);
    /** Heading text (#0F172A). */
    public static final Color HEADING_TEXT = new Color(0x0F172A);
    /** Inactive nav text (#94A3B8). */
    public static final Color INACTIVE_NAV_TEXT = new Color(0x94A3B8);
    /** Inactive nav text hover (#F1F5F9). */
    public static final Color INACTIVE_NAV_HOVER = new Color(0xF1F5F9);
    /** Soft red destructive (#EF4444). */
    public static final Color DESTRUCTIVE = new Color(0xDC2626);
    /** Destructive fill tint (#FEF2F2). */
    public static final Color DESTRUCTIVE_TINT = new Color(0xFEF2F2);
    /** Destructive border (#FCA5A5). */
    public static final Color DESTRUCTIVE_BORDER = new Color(0xFCA5A5);

    /** Score badge colours (foregrounds & soft pill tints). */
    public static final Color SCORE_HIGH = new Color(0x10B981);
    public static final Color SCORE_HIGH_BG = new Color(0xDCFCE7);
    public static final Color SCORE_HIGH_FG = new Color(0x15803D);

    public static final Color SCORE_MID = new Color(0x14B8A6);
    public static final Color SCORE_MID_BG = new Color(0xCCFBF1);
    public static final Color SCORE_MID_FG = new Color(0x0F766E);

    public static final Color SCORE_WARN = new Color(0xF59E0B);
    public static final Color SCORE_WARN_BG = new Color(0xFEF3C7);
    public static final Color SCORE_WARN_FG = new Color(0xB45309);

    public static final Color SCORE_LOW = new Color(0xEF4444);
    public static final Color SCORE_LOW_BG = new Color(0xFEE2E2);
    public static final Color SCORE_LOW_FG = new Color(0xB91C1C);

    /** Status badge colours. */
    public static final Color STATUS_COMPLETED = new Color(0x10B981);
    public static final Color STATUS_IN_PROGRESS = new Color(0xF59E0B);
    public static final Color STATUS_PENDING = new Color(0xF59E0B);

    /** Available vector icon shapes. */
    public enum Symbol {
        BRAND_CUBE,
        EVALUATION,
        GOAL,
        FEEDBACK,
        REPORT,
        TEAM,
        LOGOUT,
        STAR,
        CLIPBOARD,
        TROPHY,
        TARGET,
        PLUS,
        EDIT,
        REFRESH,
        TRASH,
        DOWNLOAD,
        SEARCH,
        CHECK
    }

    private Icons() {
        // Utility class - not instantiable.
    }

    /**
     * Applies a consistent look to a primary action button.
     *
     * @param button the button to style
     * @param text   the label to set
     */
    public static void stylePrimary(AbstractButton button, String text) {
        button.setText(text);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setFocusPainted(false);
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("FlatLaf.style",
                "arc: 10; background: #4F46E5; foreground: #FFFFFF; "
                        + "hoverBackground: #4338CA; pressedBackground: #3730A3; "
                        + "borderWidth: 0; margin: 7,15,7,15");
    }

    /**
     * Applies a consistent look to a secondary/outline action button.
     *
     * @param button the button to style
     * @param text   the label to set
     */
    public static void styleSecondary(AbstractButton button, String text) {
        button.setText(text);
        button.setForeground(BODY_TEXT);
        button.setBackground(CARD_BG);
        button.setFocusPainted(false);
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("FlatLaf.style",
                "arc: 10; background: #FFFFFF; foreground: #334155; "
                        + "hoverBackground: #F8FAFC; pressedBackground: #F1F5F9; "
                        + "borderColor: #E2E8F0; borderWidth: 1; margin: 7,13,7,13");
    }

    /**
     * Applies a consistent look to a destructive action button.
     *
     * @param button the button to style
     * @param text   the label to set
     */
    public static void styleDestructive(AbstractButton button, String text) {
        button.setText(text);
        button.setForeground(DESTRUCTIVE);
        button.setBackground(DESTRUCTIVE_TINT);
        button.setFocusPainted(false);
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("FlatLaf.style",
                "arc: 10; background: #FEF2F2; foreground: #DC2626; "
                        + "hoverBackground: #FEE2E2; pressedBackground: #FECACA; "
                        + "borderColor: #FCA5A5; borderWidth: 1; margin: 7,13,7,13");
    }

    /**
     * Applies a consistent look to an action button with explicit colours.
     *
     * @param button     the button to style
     * @param text       the label to set
     * @param background the button's background colour
     * @param foreground the label colour
     */
    public static void styleButton(AbstractButton button, String text, Color background, Color foreground) {
        button.setText(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("FlatLaf.style", "arc: 10; margin: 7,14,7,14");
    }

    /**
     * Styles a text field as a clean, rounded input while preserving FlatLaf's border.
     *
     * @param field the field to style
     */
    public static void styleInput(JTextField field) {
        field.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        field.setMargin(new Insets(4, 8, 4, 8));
        field.putClientProperty("FlatLaf.style",
                "arc: 10; background: #FFFFFF; borderColor: #E2E8F0; focusedBorderColor: #4F46E5");
    }

    /**
     * Removes the default decoration from a component so it can be painted as a
     * clean card surface.
     *
     * @param component the component to clean
     */
    public static void styleCard(JComponent component) {
        component.setBackground(CARD_BG);
        component.setOpaque(true);
    }

    /**
     * Creates a crisp anti-aliased vector icon of the requested symbol.
     *
     * @param symbol the icon symbol
     * @param size   the width and height in pixels
     * @param colour the stroke/fill colour
     * @return the rendered Swing {@link Icon}
     */
    public static Icon vector(Symbol symbol, int size, Color colour) {
        return new VectorIcon(symbol, size, colour);
    }

    /**
     * Returns a font of the given size in the application family.
     *
     * @param size the font size
     * @return the font
     */
    public static Font font(float size) {
        return new Font(FONT_FAMILY, Font.PLAIN, Math.round(size));
    }

    /**
     * Returns a bold font of the given size in the application family.
     *
     * @param size the font size
     * @return the font
     */
    public static Font bold(float size) {
        return new Font(FONT_FAMILY, Font.BOLD, Math.round(size));
    }

    /**
     * Returns a soft tinted background for a KPI badge colour.
     *
     * @param accent the accent colour
     * @return a soft tint suitable behind a dark/vivid icon
     */
    public static Color softTint(Color accent) {
        if (accent == null) {
            return PRIMARY_TINT;
        }
        int r = Math.min(255, (accent.getRed() + 255 * 4) / 5);
        int g = Math.min(255, (accent.getGreen() + 255 * 4) / 5);
        int b = Math.min(255, (accent.getBlue() + 255 * 4) / 5);
        return new Color(r, g, b);
    }

    /**
     * Returns a colour for a numeric score: emerald for high, teal for solid,
     * amber for mid, rose for low.
     *
     * @param score the score, 1-5
     * @return the badge accent colour
     */
    public static Color scoreColor(double score) {
        if (score >= 4.5) {
            return SCORE_HIGH;
        }
        if (score >= 4.0) {
            return SCORE_MID;
        }
        if (score >= 3.0) {
            return SCORE_WARN;
        }
        return SCORE_LOW;
    }

    /**
     * Returns the pill background colour for a numeric score.
     */
    public static Color scorePillBg(double score) {
        if (score >= 4.5) {
            return SCORE_HIGH_BG;
        }
        if (score >= 4.0) {
            return SCORE_MID_BG;
        }
        if (score >= 3.0) {
            return SCORE_WARN_BG;
        }
        return SCORE_LOW_BG;
    }

    /**
     * Returns the pill text colour for a numeric score.
     */
    public static Color scorePillFg(double score) {
        if (score >= 4.5) {
            return SCORE_HIGH_FG;
        }
        if (score >= 4.0) {
            return SCORE_MID_FG;
        }
        if (score >= 3.0) {
            return SCORE_WARN_FG;
        }
        return SCORE_LOW_FG;
    }

    /**
     * Returns a colour for a goal/rating status.
     *
     * @param status the status text
     * @return the badge colour
     */
    public static Color statusColor(String status) {
        if (status != null) {
            switch (status) {
                case "Completed" -> {
                    return STATUS_COMPLETED;
                }
                case "In Progress" -> {
                    return STATUS_IN_PROGRESS;
                }
            }
        }
        return STATUS_PENDING;
    }

    /**
     * Returns the pill background colour for a goal status.
     */
    public static Color statusPillBg(String status) {
        if ("Completed".equalsIgnoreCase(status)) {
            return SCORE_HIGH_BG;
        }
        if ("In Progress".equalsIgnoreCase(status)) {
            return new Color(0xDBEAFE);
        }
        return SCORE_WARN_BG;
    }

    /**
     * Returns the pill text colour for a goal status.
     */
    public static Color statusPillFg(String status) {
        if ("Completed".equalsIgnoreCase(status)) {
            return SCORE_HIGH_FG;
        }
        if ("In Progress".equalsIgnoreCase(status)) {
            return new Color(0x1D4ED8);
        }
        return SCORE_WARN_FG;
    }

    /** Known goal / status values. */
    public enum GoalStatus {
        PENDING("Pending"),
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed");

        private final String label;

        GoalStatus(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Lightweight anti-aliased vector icon renderer so buttons, sidebar items,
     * and KPI cards never fall back to plain letters or empty squares.
     */
    private static final class VectorIcon implements Icon {

        private final Symbol symbol;
        private final int size;
        private final Color colour;

        VectorIcon(Symbol symbol, int size, Color colour) {
            this.symbol = symbol;
            this.size = size;
            this.colour = colour == null ? BODY_TEXT : colour;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double s = size / 16.0;
                g2.scale(s, s);
                g2.setColor(colour);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawSymbol(g2);
            } finally {
                g2.dispose();
            }
        }

        private void drawSymbol(Graphics2D g2) {
            switch (symbol) {
                case BRAND_CUBE -> {
                    Path2D hex = new Path2D.Double();
                    hex.moveTo(8, 1.5);
                    hex.lineTo(14, 4.8);
                    hex.lineTo(14, 11.2);
                    hex.lineTo(8, 14.5);
                    hex.lineTo(2, 11.2);
                    hex.lineTo(2, 4.8);
                    hex.closePath();
                    g2.draw(hex);
                    g2.draw(new Line2D.Double(2, 4.8, 8, 8));
                    g2.draw(new Line2D.Double(14, 4.8, 8, 8));
                    g2.draw(new Line2D.Double(8, 8, 8, 14.5));
                }
                case EVALUATION -> {
                    g2.draw(new RoundRectangle2D.Double(2, 3, 12, 10, 3, 3));
                    Path2D pulse = new Path2D.Double();
                    pulse.moveTo(4, 9);
                    pulse.lineTo(6.5, 9);
                    pulse.lineTo(8, 6);
                    pulse.lineTo(10, 10.5);
                    pulse.lineTo(12, 8);
                    g2.draw(pulse);
                }
                case GOAL, TARGET -> {
                    g2.draw(new Ellipse2D.Double(2, 2, 12, 12));
                    g2.draw(new Ellipse2D.Double(5, 5, 6, 6));
                    g2.fill(new Ellipse2D.Double(7, 7, 2, 2));
                }
                case FEEDBACK -> {
                    Path2D bubble = new Path2D.Double();
                    bubble.moveTo(3, 3);
                    bubble.lineTo(13, 3);
                    bubble.quadTo(14.5, 3, 14.5, 4.5);
                    bubble.lineTo(14.5, 10);
                    bubble.quadTo(14.5, 11.5, 13, 11.5);
                    bubble.lineTo(6.5, 11.5);
                    bubble.lineTo(3, 14);
                    bubble.lineTo(3, 11.5);
                    bubble.quadTo(1.5, 11.5, 1.5, 10);
                    bubble.lineTo(1.5, 4.5);
                    bubble.quadTo(1.5, 3, 3, 3);
                    g2.draw(bubble);
                    g2.draw(new Line2D.Double(4.5, 6.2, 11.5, 6.2));
                    g2.draw(new Line2D.Double(4.5, 8.8, 9.5, 8.8));
                }
                case REPORT -> {
                    g2.draw(new RoundRectangle2D.Double(2.5, 2, 11, 12, 2, 2));
                    g2.draw(new Line2D.Double(5.5, 11, 5.5, 8.5));
                    g2.draw(new Line2D.Double(8, 11, 8, 5.5));
                    g2.draw(new Line2D.Double(10.5, 11, 10.5, 7));
                }
                case TEAM -> {
                    g2.draw(new Ellipse2D.Double(3.5, 3, 4, 4));
                    g2.draw(new Arc2D.Double(1.5, 8.5, 8, 6, 0, 180, Arc2D.OPEN));
                    g2.draw(new Ellipse2D.Double(9.5, 3.8, 3.5, 3.5));
                    g2.draw(new Arc2D.Double(8.5, 9, 6, 5, 0, 160, Arc2D.OPEN));
                }
                case LOGOUT -> {
                    g2.draw(new Arc2D.Double(2, 2, 8, 12, 90, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(6.5, 8, 14, 8));
                    g2.draw(new Line2D.Double(11, 5, 14, 8));
                    g2.draw(new Line2D.Double(11, 11, 14, 8));
                }
                case STAR -> {
                    Path2D star = new Path2D.Double();
                    double cx = 8, cy = 8, outer = 6.2, inner = 2.7;
                    for (int i = 0; i < 10; i++) {
                        double angle = -Math.PI / 2 + i * Math.PI / 5;
                        double r = (i % 2 == 0) ? outer : inner;
                        double px = cx + r * Math.cos(angle);
                        double py = cy + r * Math.sin(angle);
                        if (i == 0) star.moveTo(px, py);
                        else star.lineTo(px, py);
                    }
                    star.closePath();
                    g2.draw(star);
                }
                case CLIPBOARD -> {
                    g2.draw(new RoundRectangle2D.Double(3, 3, 10, 11, 2, 2));
                    g2.draw(new RoundRectangle2D.Double(6, 1.5, 4, 2.5, 1, 1));
                    g2.draw(new Line2D.Double(5.5, 7, 10.5, 7));
                    g2.draw(new Line2D.Double(5.5, 10, 9.5, 10));
                }
                case TROPHY -> {
                    g2.draw(new Arc2D.Double(4, 2.5, 8, 7.5, 180, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(4, 2.5, 12, 2.5));
                    g2.draw(new Line2D.Double(4, 2.5, 4, 6));
                    g2.draw(new Line2D.Double(12, 2.5, 12, 6));
                    g2.draw(new Arc2D.Double(2, 3.2, 2, 3.5, 90, 180, Arc2D.OPEN));
                    g2.draw(new Arc2D.Double(12, 3.2, 2, 3.5, -90, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(8, 10, 8, 13.5));
                    g2.draw(new Line2D.Double(5.5, 13.5, 10.5, 13.5));
                }
                case PLUS -> {
                    g2.draw(new Line2D.Double(8, 3, 8, 13));
                    g2.draw(new Line2D.Double(3, 8, 13, 8));
                }
                case EDIT -> {
                    Path2D pen = new Path2D.Double();
                    pen.moveTo(3, 13);
                    pen.lineTo(3.5, 10);
                    pen.lineTo(11.5, 2);
                    pen.lineTo(14, 4.5);
                    pen.lineTo(6, 12.5);
                    pen.closePath();
                    g2.draw(pen);
                }
                case REFRESH -> {
                    g2.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 35, 280, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(12.5, 2.5, 13, 6));
                    g2.draw(new Line2D.Double(13, 6, 9.5, 5.8));
                }
                case TRASH -> {
                    g2.draw(new Line2D.Double(2.5, 4, 13.5, 4));
                    g2.draw(new Line2D.Double(6, 2, 10, 2));
                    g2.draw(new RoundRectangle2D.Double(4, 4, 8, 9.5, 1.5, 1.5));
                    g2.draw(new Line2D.Double(6.5, 6.5, 6.5, 11));
                    g2.draw(new Line2D.Double(9.5, 6.5, 9.5, 11));
                }
                case DOWNLOAD -> {
                    g2.draw(new Line2D.Double(8, 2.5, 8, 10.5));
                    g2.draw(new Line2D.Double(5, 7.5, 8, 10.5));
                    g2.draw(new Line2D.Double(11, 7.5, 8, 10.5));
                    g2.draw(new Line2D.Double(2.5, 13, 13.5, 13));
                }
                case SEARCH -> {
                    g2.draw(new Ellipse2D.Double(2.5, 2.5, 8.5, 8.5));
                    g2.draw(new Line2D.Double(9.8, 9.8, 13.5, 13.5));
                }
                case CHECK -> {
                    g2.draw(new Line2D.Double(3, 8.5, 6.5, 12));
                    g2.draw(new Line2D.Double(6.5, 12, 13, 4.5));
                }
            }
        }
    }
}
