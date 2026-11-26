package ui;

import dao.DatabaseConnection;
import dao.UserDAO;
import model.User;
import ui.components.RoundedPanel;
import util.FileLogger;
import util.Icons;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/**
 * Login window and role based entry point into the two dashboards.
 *
 * <p>The login form lives on a single rounded white card centred on the
 * {@code #F4F6FA} canvas, matching the Light SaaS Sidebar Redesign.
 * Authentication is performed against the {@code users} table using
 * {@link dao.UserDAO#authenticate(String, String, String)}. Passwords are
 * compared against a salted PBKDF2 hash; legacy hashes are upgraded
 * transparently on the first successful sign-in.
 */
public final class LoginFrame extends JFrame {

    /** Serialisation version - required for a {@link java.io.Serializable} subclass. */
    private static final long serialVersionUID = 1L;

    private final JTextField emailField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"MANAGER", "EMPLOYEE"});
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("Login");

    /**
     * Builds the login window.
     */
    public LoginFrame() {
        setTitle("Performetrics - Login");
        setSize(460, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Outer canvas: the #F4F6FA background.
        JPanel canvas = new JPanel(new BorderLayout());
        canvas.setBackground(Icons.CANVAS_BG);
        canvas.setBorder(new javax.swing.border.EmptyBorder(20, 20, 20, 20));

        // Centring wrapper: places the card in the middle of the canvas.
        JPanel centre = new JPanel(new GridBagLayout());
        centre.setBackground(Icons.CANVAS_BG);
        centre.setAlignmentX(0.5f);
        centre.setAlignmentY(0.5f);

        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(Icons.CARD_BG);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new java.awt.Dimension(360, 520));
        card.setMaximumSize(new java.awt.Dimension(360, 520));
        card.setBorder(new javax.swing.border.EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;

        // Brand mark.
        RoundedPanel brandMark = new RoundedPanel(12, null, 0);
        brandMark.setBackground(Icons.PRIMARY);
        brandMark.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
        brandMark.setLayout(new BorderLayout());
        brandMark.setPreferredSize(new java.awt.Dimension(46, 46));
        brandMark.setMinimumSize(new java.awt.Dimension(46, 46));
        brandMark.add(new JLabel(Icons.vector(Icons.Symbol.BRAND_CUBE, 24, Color.WHITE), SwingConstants.CENTER),
                BorderLayout.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(brandMark, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title.
        JLabel title = new JLabel("Performetrics", SwingConstants.CENTER);
        title.setFont(Icons.bold(24f));
        title.setForeground(Icons.HEADING_TEXT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(title, gbc);

        // Subtitle.
        JLabel subtitle = new JLabel("Sign in to your performance dashboard", SwingConstants.CENTER);
        subtitle.setFont(Icons.font(13f));
        subtitle.setForeground(Icons.MUTED_TEXT);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 26, 0);
        card.add(subtitle, gbc);

        // Email field.
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.NORTH;
        JLabel emailLabel = new JLabel("E-mail address");
        emailLabel.setFont(Icons.font(12f).deriveFont(java.awt.Font.BOLD));
        emailLabel.setForeground(Icons.BODY_TEXT);
        card.add(emailLabel, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(4, 0, 16, 0);
        Icons.styleInput(emailField);
        emailField.putClientProperty("JTextField.placeholderText", "name@company.com");
        card.add(emailField, gbc);

        // Password field.
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(Icons.font(12f).deriveFont(java.awt.Font.BOLD));
        passwordLabel.setForeground(Icons.BODY_TEXT);
        card.add(passwordLabel, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(4, 0, 16, 0);
        Icons.styleInput(passwordField);
        passwordField.putClientProperty("JTextField.placeholderText", "••••••••");
        card.add(passwordField, gbc);

        // Role selector.
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel roleLabel = new JLabel("Role");
        roleLabel.setFont(Icons.font(12f).deriveFont(java.awt.Font.BOLD));
        roleLabel.setForeground(Icons.BODY_TEXT);
        card.add(roleLabel, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(4, 0, 22, 0);
        roleCombo.setFont(Icons.font(13f));
        roleCombo.setBorder(new javax.swing.border.EmptyBorder(8, 12, 8, 12));
        card.add(roleCombo, gbc);

        // Login button.
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 0, 0);
        Icons.stylePrimary(loginButton, "Sign in");
        loginButton.setPreferredSize(new java.awt.Dimension(0, 44));
        loginButton.addActionListener(event -> doLogin());
        card.add(loginButton, gbc);

        // Status label.
        gbc.gridy = 10;
        gbc.insets = new Insets(10, 0, 0, 0);
        statusLabel.setFont(Icons.font(11f).deriveFont(java.awt.Font.ITALIC));
        statusLabel.setForeground(Icons.MUTED_TEXT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(statusLabel, gbc);

        centre.add(card, new GridBagConstraints());
        canvas.add(centre, BorderLayout.CENTER);
        add(canvas, BorderLayout.CENTER);

        // The Enter key submits the form.
        getRootPane().setDefaultButton(loginButton);

        if (!DatabaseConnection.isDriverAvailable()) {
            statusLabel.setText("MySQL driver missing from lib/");
        }
    }

    /**
     * Authenticates the user and opens the dashboard matching their role.
     */
    private void doLogin() {
        String role = (String) roleCombo.getSelectedItem();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter both an e-mail address and a password.");
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setText("Signing in...");

        // Authentication touches the database, so run it off the event thread.
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return new UserDAO().authenticate(email, password, role);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    User user = get();
                    if (user == null) {
                        statusLabel.setText("Invalid credentials for role " + role);
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Invalid e-mail, password or role.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    FileLogger.logInfo("Login", user.getEmail() + " signed in as " + user.getRole());
                    openDashboard(user);
                } catch (ExecutionException e) {
                    // A background failure arrives wrapped - unwrap it to show the real reason.
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    if (cause instanceof SQLException) {
                        statusLabel.setText("Database unreachable");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Could not reach the database.\nStart MySQL and try again.\n\n"
                                        + cause.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        statusLabel.setText("Login failed");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Login failed: " + cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    FileLogger.logError("Login failed for " + email,
                            cause instanceof Exception ? (Exception) cause : e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    statusLabel.setText("Login failed");
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Login failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    FileLogger.logError("Login failed for " + email, e);
                }
            }
        }.execute();
    }

    /**
     * Opens the dashboard that matches the authenticated user's role.
     */
    private void openDashboard(User user) {
        FileLogger.logInfo("Login", user.describe() + " signed in (id " + user.getUserId()
                + ") -> " + user.getDashboardType());
        dispose();
        if (user instanceof model.Manager) {
            new ManagerDashboard(user.getName(), user.getUserId()).setVisible(true);
        } else {
            new EmployeeDashboard(user.getName(), user.getUserId()).setVisible(true);
        }
    }

    /**
     * Allows the login window to be started on its own.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
