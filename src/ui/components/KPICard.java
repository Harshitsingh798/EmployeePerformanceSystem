package ui.components;

import util.Icons;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * A KPI summary card: a title, a bold metric value, an optional subtitle, and a
 * soft-tinted rounded vector icon badge.
 */
public class KPICard extends RoundedPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel = new JLabel("Metric", SwingConstants.LEFT);
    private final JLabel valueLabel = new JLabel("0", SwingConstants.LEFT);
    private final JLabel subtitleLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel iconLabel = new JLabel("", SwingConstants.CENTER);

    /**
     * Creates a KPI card with a vector symbol badge.
     */
    @SuppressWarnings("this-escape")
    public KPICard(String title, String value, String subtitle, Color accentColor, Icons.Symbol symbol) {
        super(14);
        setBackground(Icons.CARD_BG);
        setLayout(new BorderLayout(10, 6));
        setBorder(new EmptyBorder(14, 16, 14, 16));

        Color accent = accentColor == null ? Icons.PRIMARY : accentColor;

        titleLabel.setFont(Icons.font(12f));
        titleLabel.setForeground(Icons.MUTED_TEXT);

        valueLabel.setFont(Icons.bold(22f));
        valueLabel.setForeground(Icons.HEADING_TEXT);

        subtitleLabel.setFont(Icons.font(11f));
        subtitleLabel.setForeground(Icons.MUTED_TEXT);

        RoundedPanel badge = new RoundedPanel(10, null, 0);
        badge.setBackground(Icons.softTint(accent));
        badge.setBorder(new EmptyBorder(0, 0, 0, 0));
        badge.setLayout(new BorderLayout());
        badge.setPreferredSize(new Dimension(34, 34));
        badge.setMinimumSize(new Dimension(34, 34));
        badge.setMaximumSize(new Dimension(34, 34));

        iconLabel.setIcon(Icons.vector(symbol == null ? Icons.Symbol.STAR : symbol, 16, accent));
        badge.add(iconLabel, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(titleLabel, BorderLayout.WEST);
        top.add(badge, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(4, 4));
        body.setOpaque(false);
        body.add(valueLabel, BorderLayout.CENTER);
        if (subtitle != null && !subtitle.isBlank()) {
            body.add(subtitleLabel, BorderLayout.SOUTH);
        }

        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        setTitle(title);
        setValue(value);
        setSubtitle(subtitle);
    }

    /**
     * Backwards-compatible constructor.
     */
    public KPICard(String title, String value, String subtitle) {
        this(title, value, subtitle, Icons.PRIMARY, Icons.Symbol.STAR);
    }

    public void setTitle(String title) {
        titleLabel.setText(title == null ? "" : title);
    }

    public void setValue(String value) {
        valueLabel.setText(value == null ? "0" : value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle == null ? "" : subtitle);
    }
}
