package ui.components;

import util.Icons;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The left navigation shell used by both dashboards.
 *
 * <p>Contains the Performetrics brand header, a compact user profile card with
 * a circular initials avatar, vector-icon navigation buttons that switch a
 * {@link CardLayout}-based workspace, and a Logout button pinned at the bottom.
 */
public class Sidebar extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Sidebar width in pixels. */
    public static final int WIDTH = 248;

    private final transient Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final transient List<NavButton> navOrder = new ArrayList<>();
    private final JPanel bottomArea = new JPanel(new BorderLayout());
    private String activeKey;

    /**
     * Creates the sidebar.
     *
     * @param userName the signed-in user's name, shown in the profile card
     * @param userRole the signed-in user's role, shown in the profile card
     * @param sections the navigation sections, in display order
     * @param onSelect called with the section key when a nav button is clicked
     */
    @SuppressWarnings("this-escape")
    public Sidebar(String userName, String userRole, Map<String, String> sections,
                   ActionListener onSelect) {
        setLayout(new BorderLayout());
        setBackground(Icons.SIDEBAR_BG);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));

        // Top stack: brand + compact profile card + navigation links.
        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setOpaque(false);
        topStack.setBorder(new EmptyBorder(18, 14, 10, 14));

        topStack.add(buildBrandHeader());
        topStack.add(Box.createVerticalStrut(16));
        topStack.add(buildProfileCard(userName, userRole));
        topStack.add(Box.createVerticalStrut(18));

        int i = 0;
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String key = entry.getKey();
            String label = entry.getValue();
            NavButton button = new NavButton(label, iconForSection(key, label));
            button.addActionListener(event -> {
                select(key);
                if (onSelect != null) {
                    onSelect.actionPerformed(event);
                }
            });
            navButtons.put(key, button);
            navOrder.add(button);
            topStack.add(button);
            topStack.add(Box.createVerticalStrut(6));
            if (i == 0) {
                select(key);
            }
            i++;
        }

        add(topStack, BorderLayout.NORTH);

        // Flexible middle spacer keeps nav items near the top and Logout at bottom.
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        add(spacer, BorderLayout.CENTER);

        bottomArea.setOpaque(false);
        bottomArea.setBorder(new EmptyBorder(8, 14, 18, 14));
        add(bottomArea, BorderLayout.SOUTH);
    }

    /**
     * Returns the navigation button at the given insertion index, so callers
     * can name buttons by position for {@link CardLayout} navigation.
     *
     * @param index the insertion index of the button
     * @return the navigation button
     */
    public JButton navButtonAt(int index) {
        return navOrder.get(index);
    }

    /**
     * Selects the navigation section with the given key, painting it as the
     * active indigo pill.
     *
     * @param key the section key
     */
    public void select(String key) {
        if (activeKey != null) {
            NavButton previous = navButtons.get(activeKey);
            if (previous != null) {
                previous.setActive(false);
            }
        }
        activeKey = key;
        NavButton current = navButtons.get(key);
        if (current != null) {
            current.setActive(true);
        }
        revalidate();
        repaint();
    }

    /**
     * Pins a Logout button at the bottom of the sidebar.
     *
     * @param onLogout the logout action
     */
    public void addLogout(ActionListener onLogout) {
        bottomArea.removeAll();
        NavButton logout = new NavButton("Logout", Icons.Symbol.LOGOUT);
        logout.addActionListener(onLogout);
        bottomArea.add(logout, BorderLayout.CENTER);
        bottomArea.revalidate();
        bottomArea.repaint();
    }

    // ------------------------------------------------------------------ builders

    private JPanel buildBrandHeader() {
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        brand.setBorder(new EmptyBorder(0, 4, 0, 4));

        RoundedPanel mark = new RoundedPanel(10, null, 0);
        mark.setBackground(Icons.PRIMARY);
        mark.setBorder(new EmptyBorder(0, 0, 0, 0));
        mark.setLayout(new BorderLayout());
        mark.setPreferredSize(new Dimension(36, 36));
        mark.setMinimumSize(new Dimension(36, 36));
        mark.setMaximumSize(new Dimension(36, 36));
        JLabel icon = new JLabel(Icons.vector(Icons.Symbol.BRAND_CUBE, 18, Color.WHITE), SwingConstants.CENTER);
        mark.add(icon, BorderLayout.CENTER);

        JLabel title = new JLabel("Performetrics");
        title.setFont(Icons.bold(17f));
        title.setForeground(Color.WHITE);

        brand.add(mark, BorderLayout.WEST);
        brand.add(title, BorderLayout.CENTER);
        return brand;
    }

    private JPanel buildProfileCard(String userName, String userRole) {
        RoundedPanel profile = new RoundedPanel(12, Icons.SIDEBAR_BORDER, 1f);
        profile.setBackground(Icons.SIDEBAR_CARD);
        profile.setLayout(new BorderLayout(10, 0));
        profile.setBorder(new EmptyBorder(10, 12, 10, 12));
        profile.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        profile.setPreferredSize(new Dimension(WIDTH - 28, 64));

        AvatarBadge avatar = new AvatarBadge(initialsOf(userName));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(userName == null ? "User" : userName);
        nameLabel.setFont(Icons.bold(13f));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel roleLabel = new JLabel(userRole == null ? "" : userRole);
        roleLabel.setFont(Icons.font(11f));
        roleLabel.setForeground(Icons.INACTIVE_NAV_TEXT);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(Box.createVerticalGlue());
        info.add(nameLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(roleLabel);
        info.add(Box.createVerticalGlue());

        profile.add(avatar, BorderLayout.WEST);
        profile.add(info, BorderLayout.CENTER);
        return profile;
    }

    private static Icons.Symbol iconForSection(String key, String label) {
        String text = (key + " " + label).toLowerCase();
        if (text.contains("goal")) {
            return Icons.Symbol.GOAL;
        }
        if (text.contains("feedback")) {
            return Icons.Symbol.FEEDBACK;
        }
        if (text.contains("report")) {
            return Icons.Symbol.REPORT;
        }
        if (text.contains("team")) {
            return Icons.Symbol.TEAM;
        }
        return Icons.Symbol.EVALUATION;
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

    /**
     * Circular 36x36 initials avatar painted cleanly without vertical stretching.
     */
    private static final class AvatarBadge extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String initials;

        AvatarBadge(String initials) {
            this.initials = initials;
            setOpaque(false);
            Dimension dim = new Dimension(38, 38);
            setPreferredSize(dim);
            setMinimumSize(dim);
            setMaximumSize(dim);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setColor(new Color(0x334155));
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(0x475569));
                g2.drawOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(Icons.bold(13f));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(initials)) / 2;
                int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Custom-painted dark sidebar navigation button with active indigo pill,
     * subtle hover tint, and matching vector icon.
     */
    private static final class NavButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Icons.Symbol symbol;
        private boolean active;
        private boolean hovered;

        NavButton(String label, Icons.Symbol symbol) {
            super(label);
            this.symbol = symbol;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(10);
            setFont(Icons.font(13f));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setPreferredSize(new Dimension(WIDTH - 28, 42));
            updateVisualState();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    updateVisualState();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    updateVisualState();
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            updateVisualState();
            repaint();
        }

        private void updateVisualState() {
            Color fg = active ? Color.WHITE
                    : (hovered ? Icons.INACTIVE_NAV_HOVER : Icons.INACTIVE_NAV_TEXT);
            setForeground(fg);
            setFont(active ? Icons.bold(13f) : Icons.font(13f));
            setIcon(Icons.vector(symbol, 16, fg));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(active ? Icons.ACTIVE_PILL : Icons.SIDEBAR_CARD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }
    }
}
