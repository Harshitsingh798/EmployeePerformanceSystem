package ui;

import dao.DatabaseConnection;
import dao.UserDAO;
import model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Login Frame - Entry point with role-based routing
 */
public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    public LoginFrame() {
        setTitle("Performetrics - Login");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Title
        JLabel title = new JLabel("Employee Performance System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        add(title, BorderLayout.NORTH);

        // Form panel
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(18);
        emailField.setText("manager@company.com");
        form.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(18);
        passwordField.setText("manager123");
        form.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        roleCombo = new JComboBox<>(new String[]{"MANAGER", "EMPLOYEE"});
        form.add(roleCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(41, 98, 255));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginBtn.addActionListener(e -> doLogin());
        form.add(loginBtn, gbc);

        gbc.gridy = 4;
        JLabel hint = new JLabel("Demo: manager@company.com / manager123  or  priya@company.com / emp123", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hint.setForeground(Color.GRAY);
        form.add(hint, gbc);

        add(form, BorderLayout.CENTER);
    }

    private void doLogin() {
        String role = (String) roleCombo.getSelectedItem();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter email and password");
            return;
        }
        // Real DB authentication with fallback
        User authenticated = null;
        try {
            if (DatabaseConnection.getInstance().testConnection()) {
                authenticated = new UserDAO().authenticate(email, password, role);
            }
        } catch (Exception ex) {
            System.err.println("DB auth error: " + ex.getMessage());
        }
        if (authenticated == null && !DatabaseConnection.getInstance().testConnection()) {
            // Fallback when DB unreachable: allow demo credentials
            boolean demoOk = ("manager@company.com".equals(email) && "manager123".equals(password) && "MANAGER".equals(role))
                    || (("priya@company.com".equals(email) || "amit@company.com".equals(email) || "sneha@company.com".equals(email)) && "emp123".equals(password) && "EMPLOYEE".equals(role));
            if (!demoOk) {
                JOptionPane.showMessageDialog(this, "Database unreachable and demo credentials do not match.\nStart MySQL and try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Create lightweight user for UI title
            authenticated = null;
        } else if (authenticated == null) {
            JOptionPane.showMessageDialog(this, "Invalid credentials for role " + role, "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String displayName = authenticated != null ? authenticated.getName() : email;
        int userId = authenticated != null ? authenticated.getUserId() : ("manager@company.com".equals(email) ? 1 : 2);
        dispose();
        if ("MANAGER".equals(role)) {
            new ManagerDashboard(displayName, userId).setVisible(true);
        } else {
            new EmployeeDashboard(displayName, userId).setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
