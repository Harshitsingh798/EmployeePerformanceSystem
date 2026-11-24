package ui;

import dao.DatabaseConnection;
import dao.UserDAO;
import model.User;
import util.FileLogger;

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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/**
 * Login window and role based entry point into the two dashboards.
 *
 * <p>Authentication is performed against the {@code users} table using
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
        setSize(450, 360);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Employee Performance System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        form.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        form.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        form.add(roleCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginButton.setBackground(new Color(41, 98, 255));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginButton.addActionListener(event -> doLogin());
        form.add(loginButton, gbc);

        gbc.gridy = 4;
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);
        form.add(statusLabel, gbc);

        add(form, BorderLayout.CENTER);

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
