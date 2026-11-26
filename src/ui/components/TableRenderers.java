package ui.components;

import util.Icons;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;

/**
 * Reusable Java-2D table cell renderers for the Performetrics dashboards.
 *
 * <p>Avoids Swing HTML quirks by painting circular initial avatars and rounded
 * score/status pill badges directly via {@link Graphics2D}.
 */
public final class TableRenderers {

    private static final Color ROW_SELECTED_BG = new Color(0xEEF2FF);
    private static final Color[] AVATAR_COLORS = {
            new Color(0x4F46E5), // indigo
            new Color(0x2563EB), // blue
            new Color(0xDB2777), // pink
            new Color(0x0D9488), // teal
            new Color(0xD97706), // amber
            new Color(0x7C3AED)  // violet
    };

    private TableRenderers() {
        // Utility class - not instantiable.
    }

    /**
     * Table header: uppercase, muted, left-aligned with horizontal padding.
     */
    public static TableCellRenderer header() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, false, false, row, column);
                String text = value == null ? "" : value.toString();
                label.setText(text.toUpperCase(Locale.ROOT));
                label.setFont(Icons.bold(11f));
                label.setForeground(Icons.MUTED_TEXT);
                label.setBackground(Color.WHITE);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(new EmptyBorder(0, 12, 0, 12));
                label.setOpaque(true);
                return label;
            }
        };
    }

    /**
     * Standard body cell: horizontal padding, soft selection tint, no focus box.
     */
    public static TableCellRenderer cell() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, false, row, column);
                label.setText(value == null ? "" : value.toString());
                label.setFont(Icons.font(13f));
                label.setForeground(isSelected ? Icons.HEADING_TEXT : Icons.BODY_TEXT);
                label.setBackground(isSelected ? ROW_SELECTED_BG : Color.WHITE);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(new EmptyBorder(0, 12, 0, 12));
                label.setOpaque(true);
                return label;
            }
        };
    }

    /**
     * Employee column: circular initials avatar painted beside the employee name.
     */
    public static TableCellRenderer employee() {
        return new EmployeeCellRenderer();
    }

    /**
     * Score column: numeric value painted inside a soft rounded pill badge.
     */
    public static TableCellRenderer score() {
        return new PillCellRenderer(true);
    }

    /**
     * Status column: status text painted inside a soft rounded pill badge.
     */
    public static TableCellRenderer status() {
        return new PillCellRenderer(false);
    }

    // ------------------------------------------------------------------ renderers

    private static final class EmployeeCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private String employeeName = "";
        private String initials = "?";
        private Color avatarBg = AVATAR_COLORS[0];

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
            employeeName = value == null ? "" : value.toString().trim();
            initials = initialsOf(employeeName);
            avatarBg = AVATAR_COLORS[Math.abs(employeeName.hashCode()) % AVATAR_COLORS.length];
            setBackground(isSelected ? ROW_SELECTED_BG : Color.WHITE);
            setOpaque(true);
            setText("");
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());

                int h = getHeight();
                int circleSize = 28;
                int cx = 12;
                int cy = (h - circleSize) / 2;

                g2.setColor(avatarBg);
                g2.fillOval(cx, cy, circleSize, circleSize);

                g2.setColor(Color.WHITE);
                g2.setFont(Icons.bold(11f));
                FontMetrics fmInit = g2.getFontMetrics();
                int ix = cx + (circleSize - fmInit.stringWidth(initials)) / 2;
                int iy = cy + (circleSize - fmInit.getHeight()) / 2 + fmInit.getAscent();
                g2.drawString(initials, ix, iy);

                g2.setColor(Icons.HEADING_TEXT);
                g2.setFont(Icons.bold(13f));
                FontMetrics fmName = g2.getFontMetrics();
                int nx = cx + circleSize + 10;
                int ny = (h - fmName.getHeight()) / 2 + fmName.getAscent();
                g2.drawString(employeeName, nx, ny);
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class PillCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final boolean numericScore;
        private String pillText = "";
        private Color pillBg = Icons.PRIMARY_TINT;
        private Color pillFg = Icons.PRIMARY;

        PillCellRenderer(boolean numericScore) {
            this.numericScore = numericScore;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
            setBackground(isSelected ? ROW_SELECTED_BG : Color.WHITE);
            setOpaque(true);
            setText("");

            String raw = value == null ? "" : value.toString().trim();
            if (numericScore) {
                double parsed = 0.0;
                boolean valid = false;
                try {
                    parsed = Double.parseDouble(raw);
                    valid = true;
                } catch (NumberFormatException ignored) {
                }
                if (valid) {
                    pillText = raw.contains(".") ? String.format(Locale.ROOT, "%.2f", parsed) : raw;
                    pillBg = Icons.scorePillBg(parsed);
                    pillFg = Icons.scorePillFg(parsed);
                } else {
                    pillText = raw.isEmpty() ? "--" : raw;
                    pillBg = Icons.ROW_DIVIDER;
                    pillFg = Icons.MUTED_TEXT;
                }
            } else {
                pillText = raw.isEmpty() ? "Pending" : raw;
                pillBg = Icons.statusPillBg(pillText);
                pillFg = Icons.statusPillFg(pillText);
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (!pillText.isEmpty()) {
                    g2.setFont(Icons.bold(12f));
                    FontMetrics fm = g2.getFontMetrics();
                    int padX = 12;
                    int pillW = Math.min(getWidth() - 16, fm.stringWidth(pillText) + padX * 2);
                    int pillH = 24;
                    int px = 12;
                    int py = (getHeight() - pillH) / 2;

                    g2.setColor(pillBg);
                    g2.fillRoundRect(px, py, pillW, pillH, 12, 12);

                    g2.setColor(pillFg);
                    int tx = px + (pillW - fm.stringWidth(pillText)) / 2;
                    int ty = py + (pillH - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(pillText, tx, ty);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static String initialsOf(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        initials.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) {
            initials.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return initials.toString();
    }
}
