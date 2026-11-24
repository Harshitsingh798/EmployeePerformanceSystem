package ui;

import dao.EvaluationDAO;
import dao.FeedbackDAO;
import dao.GoalDAO;
import dao.TeamDAO;
import dao.UserDAO;
import exception.GoalDeadlineException;
import exception.InvalidRatingException;
import model.Employee;
import model.Evaluation;
import model.Feedback;
import model.Goal;
import model.Team;
import util.FileLogger;
import util.Icons;
import util.Validator;
import ui.components.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The manager's workspace.
 *
 * <p>Five tabs - evaluation, goals, feedback, reports and team management -
 * all backed by the live database. Every query is scoped to the signed-in
 * manager, so a manager only ever sees their own team.
 *
 * <p>All database access happens on a {@link SwingWorker}, so the user
 * interface never blocks while a query runs.
 */
public final class ManagerDashboard extends JFrame {

    /** Serialisation version - required for a {@link java.io.Serializable} subclass. */
    private static final long serialVersionUID = 1L;

    /** Delay before a typed search is sent to the database. */
    private static final int SEARCH_DEBOUNCE_MS = 300;

    private final String managerName;
    private final int managerId;

    private final DefaultTableModel evalModel;
    private final DefaultTableModel goalModel;
    private final DefaultTableModel feedbackModel;
    private final DefaultTableModel teamModel;
    private final JTable evalTable;
    private final JTable goalTable;
    private final JTable feedbackTable;
    private final JTable teamTable;

    private final JLabel statusLabel = new JLabel("  Loading data from database...");
    private final JPanel reportBarSlot = new JPanel(new BorderLayout());
    private final JPanel reportPieSlot = new JPanel(new BorderLayout());

    /** The search worker currently in flight, so a new keystroke can cancel it. */
    private transient SwingWorker<List<Evaluation>, Void> searchWorker;

    /** Debounce timer that delays the search until typing stops. */
    private final Timer searchTimer;

    /** The employees reporting to this manager - used to populate the pickers. */
    private transient List<Employee> teamMembers = new ArrayList<>();

    /**
     * Builds the dashboard.
     *
     * @param managerName the name shown in the header
     * @param managerId   the signed-in manager's user id
     */
    public ManagerDashboard(String managerName, int managerId) {
        this.managerName = managerName;
        this.managerId = managerId;

        setTitle("Manager Dashboard - " + this.managerName + " | Performetrics");
        setSize(1080, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        evalModel = readOnlyModel("Eval ID", "Employee", "Comm", "Tech", "Punct", "Team", "Score", "Date", "Comments");
        goalModel = readOnlyModel("Goal ID", "Title", "Assigned To", "Target Date", "Status", "Personal?");
        feedbackModel = readOnlyModel("Feedback ID", "Employee", "Rating", "Comments", "Date");
        teamModel = readOnlyModel("User ID", "Name", "Role", "Department", "Designation", "Email");

        evalTable = new JTable(evalModel);
        goalTable = new JTable(goalModel);
        feedbackTable = new JTable(feedbackModel);
        teamTable = new JTable(teamModel);

        searchTimer = new Timer(SEARCH_DEBOUNCE_MS, event -> runSearch(searchField.getText()));
        searchTimer.setRepeats(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(33, 33, 33));
        JLabel welcome = new JLabel("  Manager: " + this.managerName + "  (ID: " + managerId + ")");
        welcome.setForeground(Color.WHITE);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 15));
        top.add(welcome, BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(event -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        top.add(logout, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.addTab("Performance Evaluation", createEvaluationPanel());
        tabs.addTab("Goal Setting", createGoalPanel());
        tabs.addTab("Feedback Provision", createFeedbackPanel());
        tabs.addTab("Performance Reports", createReportsPanel());
        tabs.addTab("Team Management", createTeamPanel());
        add(tabs, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        add(statusLabel, BorderLayout.SOUTH);

        loadAllDataAsync();
    }

    /**
     * The search field, declared here so the debounce timer can read it.
     */
    private final JTextField searchField = new JTextField(20);

    // ------------------------------------------------------------------ panels

    /**
     * Builds the evaluation tab: a searchable table plus the CRUD buttons.
     */
    private JPanel createEvaluationPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        searchField.setToolTipText("Search by employee name or comments");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                scheduleSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                scheduleSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                scheduleSearch();
            }
        });
        top.add(new JLabel("Search: "), BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        panel.add(new JScrollPane(evalTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton add = new JButton("Add Evaluation");
        JButton edit = new JButton("Edit Selected");
        JButton refresh = new JButton("Refresh");
        JButton delete = new JButton("Delete Selected");
        add.addActionListener(event -> showAddEvaluationDialog());
        edit.addActionListener(event -> editSelectedEvaluation());
        refresh.addActionListener(event -> loadEvaluationsAsync());
        delete.addActionListener(event -> deleteSelectedEvaluation());
        Icons.styleButton(add, "Add", new Color(41, 98, 255), Color.WHITE);
        Icons.styleButton(edit, "Edit", new Color(255, 152, 0), Color.WHITE);
        Icons.styleButton(refresh, "Refresh", new Color(40, 167, 69), Color.WHITE);
        Icons.styleButton(delete, "Delete", new Color(220, 53, 69), Color.WHITE);
        buttons.add(add);
        buttons.add(edit);
        buttons.add(refresh);
        buttons.add(delete);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Builds the goal tab.
     */
    private JPanel createGoalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(goalTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton add = new JButton("Set New Goal");
        JButton edit = new JButton("Edit Selected");
        JButton refresh = new JButton("Refresh");
        add.addActionListener(event -> showAddGoalDialog());
        edit.addActionListener(event -> editSelectedGoal());
        refresh.addActionListener(event -> loadGoalsAsync());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(refresh);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Builds the feedback tab.
     */
    private JPanel createFeedbackPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(feedbackTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton add = new JButton("Give Feedback");
        JButton edit = new JButton("Edit Selected");
        JButton refresh = new JButton("Refresh");
        add.addActionListener(event -> showAddFeedbackDialog());
        edit.addActionListener(event -> editSelectedFeedback());
        refresh.addActionListener(event -> loadFeedbackAsync());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(refresh);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Builds the reports tab with the two charts and the CSV export button.
     */
    private JPanel createReportsPanel() {
        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 10));
        charts.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reportBarSlot.setBorder(BorderFactory.createTitledBorder("Team Average Scores (Bar)"));
        reportPieSlot.setBorder(BorderFactory.createTitledBorder("Criteria Breakdown (Pie)"));
        charts.add(reportBarSlot);
        charts.add(reportPieSlot);

        JButton export = new JButton("Export Team Report to CSV");
        export.addActionListener(event -> exportReport());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(charts, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.add(export);
        wrapper.add(south, BorderLayout.SOUTH);
        return wrapper;
    }

    /**
     * Builds the team management tab.
     */
    private JPanel createTeamPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(teamTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton addMember = new JButton("Add Member to Team");
        JButton assignRole = new JButton("Assign Role");
        JButton refresh = new JButton("Refresh");
        addMember.addActionListener(event -> showAddMemberDialog(false));
        assignRole.addActionListener(event -> showAddMemberDialog(true));
        refresh.addActionListener(event -> loadTeamAsync());
        buttons.add(addMember);
        buttons.add(assignRole);
        buttons.add(refresh);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------- async loading

    /**
     * Starts every background load.
     */
    private void loadAllDataAsync() {
        loadTeamAsync();
        loadEvaluationsAsync();
        loadGoalsAsync();
        loadFeedbackAsync();
        loadReportAsync();
    }

    /**
     * Loads the evaluations created by this manager.
     */
    private void loadEvaluationsAsync() {
        new SwingWorker<List<Evaluation>, Void>() {
            @Override
            protected List<Evaluation> doInBackground() throws Exception {
                return new EvaluationDAO().getByManagerId(managerId);
            }

            @Override
            protected void done() {
                try {
                    List<Evaluation> evaluations = get();
                    evalModel.setRowCount(0);
                    for (Evaluation evaluation : evaluations) {
                        evalModel.addRow(new Object[]{
                                evaluation.getEvalId(),
                                displayName(evaluation.getEmployeeName(), evaluation.getEmployeeId()),
                                evaluation.getCommunication(),
                                evaluation.getTechnical(),
                                evaluation.getPunctuality(),
                                evaluation.getTeamwork(),
                                String.format(Locale.ROOT, "%.2f", evaluation.calculateScore()),
                                evaluation.getEvalDate(),
                                evaluation.getComments()});
                    }
                    statusLabel.setText("  Evaluations loaded: " + evaluations.size());
                    FileLogger.logInfo("Load", "Manager loaded " + evaluations.size() + " evaluations");
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    statusLabel.setText("  Evaluations load failed: " + error.getMessage());
                    FileLogger.logError("Evaluation load failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    statusLabel.setText("  Evaluations load failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Loads the goals this manager has assigned.
     */
    private void loadGoalsAsync() {
        new SwingWorker<List<Goal>, Void>() {
            @Override
            protected List<Goal> doInBackground() throws Exception {
                return new GoalDAO().getAssignedByManager(managerId);
            }

            @Override
            protected void done() {
                try {
                    List<Goal> goals = get();
                    goalModel.setRowCount(0);
                    for (Goal goal : goals) {
                        goalModel.addRow(new Object[]{
                                goal.getGoalId(),
                                goal.getTitle(),
                                goal.getAssignedTo(),
                                goal.getTargetDate(),
                                goal.getStatus(),
                                goal.isPersonal() ? "Yes" : "No"});
                    }
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    statusLabel.setText("  Goals load failed: " + error.getMessage());
                    FileLogger.logError("Goal load failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    statusLabel.setText("  Goals load failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Loads the feedback this manager has written.
     */
    private void loadFeedbackAsync() {
        new SwingWorker<List<Feedback>, Void>() {
            @Override
            protected List<Feedback> doInBackground() throws Exception {
                return new FeedbackDAO().getByManagerId(managerId);
            }

            @Override
            protected void done() {
                try {
                    List<Feedback> feedbackList = get();
                    feedbackModel.setRowCount(0);
                    for (Feedback feedback : feedbackList) {
                        feedbackModel.addRow(new Object[]{
                                feedback.getFeedbackId(),
                                feedback.getEmployeeId(),
                                feedback.getRating(),
                                feedback.getComments(),
                                feedback.getGivenOn()});
                    }
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    statusLabel.setText("  Feedback load failed: " + error.getMessage());
                    FileLogger.logError("Feedback load failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    statusLabel.setText("  Feedback load failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Loads the manager's team and refreshes the pickers used by the dialogs.
     */
    private void loadTeamAsync() {
        new SwingWorker<List<Employee>, Void>() {
            @Override
            protected List<Employee> doInBackground() throws Exception {
                return new UserDAO().getEmployeesForManager(managerId);
            }

            @Override
            protected void done() {
                try {
                    List<Employee> employees = get();
                    teamMembers = employees;
                    teamModel.setRowCount(0);
                    for (Employee employee : employees) {
                        teamModel.addRow(new Object[]{
                                employee.getUserId(),
                                employee.getName(),
                                employee.getRole(),
                                employee.getDepartment(),
                                employee.getDesignation(),
                                employee.getEmail()});
                    }
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    statusLabel.setText("  Team load failed: " + error.getMessage());
                    FileLogger.logError("Team load failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    statusLabel.setText("  Team load failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Loads the chart data for the reports tab.
     */
    private void loadReportAsync() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                EvaluationDAO dao = new EvaluationDAO();
                Map<Integer, Double> averages = dao.getAverageScoresByEmployee(managerId);
                List<Evaluation> all = dao.getByManagerId(managerId);

                // Resolve the names here rather than reading teamMembers, which the
                // team loader may not have filled in yet.
                Map<Integer, String> names = new HashMap<>();
                for (Employee employee : new UserDAO().getEmployeesForManager(managerId)) {
                    names.put(employee.getUserId(), employee.getName());
                }

                Map<String, Double> barData = new LinkedHashMap<>();
                for (Map.Entry<Integer, Double> entry : averages.entrySet()) {
                    String name = names.get(entry.getKey());
                    barData.put(name == null || name.isBlank()
                            ? "Emp #" + entry.getKey() : name, entry.getValue());
                }

                double communication = 0;
                double technical = 0;
                double punctuality = 0;
                double teamwork = 0;
                for (Evaluation evaluation : all) {
                    communication += evaluation.getCommunication();
                    technical += evaluation.getTechnical();
                    punctuality += evaluation.getPunctuality();
                    teamwork += evaluation.getTeamwork();
                }
                Map<String, Double> pieData = new LinkedHashMap<>();
                if (!all.isEmpty()) {
                    double count = all.size();
                    pieData.put("Communication", communication / count);
                    pieData.put("Technical", technical / count);
                    pieData.put("Punctuality", punctuality / count);
                    pieData.put("Teamwork", teamwork / count);
                }
                return new Object[]{barData, pieData};
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    @SuppressWarnings("unchecked")
                    Map<String, Double> barData = (Map<String, Double>) result[0];
                    @SuppressWarnings("unchecked")
                    Map<String, Double> pieData = (Map<String, Double>) result[1];

                    reportBarSlot.removeAll();
                    reportBarSlot.add(ChartPanel.bar("Avg Score per Employee", "Score", barData), BorderLayout.CENTER);
                    reportPieSlot.removeAll();
                    reportPieSlot.add(ChartPanel.pie("Criteria Averages", pieData), BorderLayout.CENTER);
                    reportBarSlot.revalidate();
                    reportBarSlot.repaint();
                    reportPieSlot.revalidate();
                    reportPieSlot.repaint();
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    statusLabel.setText("  Report load failed: " + error.getMessage());
                    FileLogger.logError("Report load failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    statusLabel.setText("  Report load failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------ search

    /**
     * Restarts the debounce timer whenever the search box changes.
     */
    private void scheduleSearch() {
        searchTimer.restart();
    }

    /**
     * Runs the search, cancelling any request that is still running.
     *
     * @param text the search term
     */
    private void runSearch(String text) {
        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
        }
        searchWorker = new SwingWorker<List<Evaluation>, Void>() {
            @Override
            protected List<Evaluation> doInBackground() throws Exception {
                return new EvaluationDAO().search(text, managerId);
            }

            @Override
            protected void done() {
                try {
                    List<Evaluation> evaluations = get();
                    evalModel.setRowCount(0);
                    for (Evaluation evaluation : evaluations) {
                        evalModel.addRow(new Object[]{
                                evaluation.getEvalId(),
                                displayName(evaluation.getEmployeeName(), evaluation.getEmployeeId()),
                                evaluation.getCommunication(),
                                evaluation.getTechnical(),
                                evaluation.getPunctuality(),
                                evaluation.getTeamwork(),
                                String.format(Locale.ROOT, "%.2f", evaluation.calculateScore()),
                                evaluation.getEvalDate(),
                                evaluation.getComments()});
                    }
                    statusLabel.setText("  Filtered: " + evaluations.size() + " evaluations");
                } catch (java.util.concurrent.CancellationException e) {
                    // A newer search superseded this one - nothing to report.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    statusLabel.setText("  Filter failed: " + e.getMessage());
                }
            }
        };
        searchWorker.execute();
    }

    // ------------------------------------------------------------------ dialogs

    /**
     * Adds a member to one of the manager's teams.
     *
     * @param updateOnly {@code true} to change an existing member's role
     */
    private void showAddMemberDialog(boolean updateOnly) {
        List<Team> teams;
        try {
            teams = new TeamDAO().getByManager(managerId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load your teams: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (teams.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You do not manage any teams yet.",
                    "No Teams", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (teamMembers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no employees on your team yet.",
                    "No Employees", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, updateOnly ? "Update Member Role" : "Add Member to Team", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Team> teamPicker = new JComboBox<>(teams.toArray(new Team[0]));
        JComboBox<Employee> employeePicker = new JComboBox<>(teamMembers.toArray(new Employee[0]));
        JTextField roleField = new JTextField(15);
        roleField.setText(updateOnly ? "Lead Developer" : "Developer");

        int y = 0;
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Team:"), gbc);
        gbc.gridx = 1;
        dialog.add(teamPicker, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Employee:"), gbc);
        gbc.gridx = 1;
        dialog.add(employeePicker, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        dialog.add(roleField, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Save");
        save.addActionListener(event -> {
            Team team = (Team) teamPicker.getSelectedItem();
            Employee employee = (Employee) employeePicker.getSelectedItem();
            String role = roleField.getText().trim();
            if (team == null || employee == null) {
                JOptionPane.showMessageDialog(dialog, "Select a team and an employee.");
                return;
            }
            if (role.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Enter a role.");
                return;
            }
            try {
                TeamDAO dao = new TeamDAO();
                boolean ok = updateOnly
                        ? dao.updateMemberRole(team.getTeamId(), employee.getUserId(), role)
                        : dao.addMember(team.getTeamId(), employee.getUserId(), role);
                if (ok) {
                    FileLogger.logInfo("Team", "Employee " + employee.getUserId()
                            + " role '" + role + "' in team " + team.getTeamId());
                    JOptionPane.showMessageDialog(dialog, "Saved!");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            updateOnly ? "That employee is not a member of the selected team."
                                    : "Already a member of that team.");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Failed: " + e.getMessage());
                FileLogger.logError("Team member save failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Opens the dialog that creates a new evaluation.
     */
    private void showAddEvaluationDialog() {
        if (teamMembers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no employees on your team yet.",
                    "No Employees", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog dialog = new JDialog(this, "Add Evaluation", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Employee> employeePicker = new JComboBox<>(teamMembers.toArray(new Employee[0]));
        JComboBox<Integer> communication = ratingCombo();
        JComboBox<Integer> technical = ratingCombo();
        JComboBox<Integer> punctuality = ratingCombo();
        JComboBox<Integer> teamwork = ratingCombo();
        JTextArea comments = new JTextArea(3, 20);

        int y = 0;
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Employee:"), gbc);
        gbc.gridx = 1;
        dialog.add(employeePicker, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Communication (1-5):"), gbc);
        gbc.gridx = 1;
        dialog.add(communication, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Technical (1-5):"), gbc);
        gbc.gridx = 1;
        dialog.add(technical, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Punctuality (1-5):"), gbc);
        gbc.gridx = 1;
        dialog.add(punctuality, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Teamwork (1-5):"), gbc);
        gbc.gridx = 1;
        dialog.add(teamwork, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Comments:"), gbc);
        gbc.gridx = 1;
        dialog.add(new JScrollPane(comments), gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Save");
        save.addActionListener(event -> {
            Employee employee = (Employee) employeePicker.getSelectedItem();
            if (employee == null) {
                JOptionPane.showMessageDialog(dialog, "Select an employee.");
                return;
            }
            try {
                Evaluation evaluation = new Evaluation(
                        employee.getUserId(), managerId,
                        (int) communication.getSelectedItem(),
                        (int) technical.getSelectedItem(),
                        (int) punctuality.getSelectedItem(),
                        (int) teamwork.getSelectedItem(),
                        comments.getText().trim());
                new EvaluationDAO().add(evaluation);
                FileLogger.logInfo("Evaluation", "Manager " + managerId + " evaluated employee "
                        + employee.getUserId() + " score " + evaluation.calculateScore());
                JOptionPane.showMessageDialog(dialog, "Evaluation saved! Score: "
                        + String.format(Locale.ROOT, "%.2f", evaluation.calculateScore()));
                dialog.dispose();
                loadEvaluationsAsync();
                loadReportAsync();
            } catch (InvalidRatingException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Save failed: " + e.getMessage());
                FileLogger.logError("Evaluation save failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Opens the dialog that creates a new goal.
     */
    private void showAddGoalDialog() {
        if (teamMembers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no employees on your team yet.",
                    "No Employees", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog dialog = new JDialog(this, "Set New Goal", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField title = new JTextField(20);
        JTextArea description = new JTextArea(3, 20);
        JComboBox<Employee> employeePicker = new JComboBox<>(teamMembers.toArray(new Employee[0]));
        JTextField dateField = new JTextField(10);
        dateField.setText(LocalDate.now().plusDays(30).toString());

        int y = 0;
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        dialog.add(title, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        dialog.add(new JScrollPane(description), gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Assign to:"), gbc);
        gbc.gridx = 1;
        dialog.add(employeePicker, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Target Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dialog.add(dateField, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Save Goal");
        save.addActionListener(event -> {
            Employee employee = (Employee) employeePicker.getSelectedItem();
            if (employee == null) {
                JOptionPane.showMessageDialog(dialog, "Select an employee.");
                return;
            }
            try {
                Validator.requireText(title.getText(), "Title");
                LocalDate target = LocalDate.parse(dateField.getText().trim());
                Validator.validateGoalDeadline(target);
                Goal goal = new Goal(title.getText().trim(), description.getText().trim(),
                        employee.getUserId(), managerId, target, false);
                new GoalDAO().add(goal);
                FileLogger.logInfo("Goal", "Goal '" + goal.getTitle() + "' assigned to " + employee.getUserId());
                JOptionPane.showMessageDialog(dialog, "Goal saved!");
                dialog.dispose();
                loadGoalsAsync();
            } catch (GoalDeadlineException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (java.time.format.DateTimeParseException e) {
                JOptionPane.showMessageDialog(dialog, "Use the date format YYYY-MM-DD.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Save failed: " + e.getMessage());
                FileLogger.logError("Goal save failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Opens the dialog that records feedback for an employee.
     */
    private void showAddFeedbackDialog() {
        if (teamMembers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no employees on your team yet.",
                    "No Employees", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog dialog = new JDialog(this, "Give Feedback", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Employee> employeePicker = new JComboBox<>(teamMembers.toArray(new Employee[0]));
        JComboBox<Integer> rating = ratingCombo();
        JTextArea comments = new JTextArea(3, 20);

        int y = 0;
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Employee:"), gbc);
        gbc.gridx = 1;
        dialog.add(employeePicker, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Rating (1-5):"), gbc);
        gbc.gridx = 1;
        dialog.add(rating, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Comments:"), gbc);
        gbc.gridx = 1;
        dialog.add(new JScrollPane(comments), gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Submit Feedback");
        save.addActionListener(event -> {
            Employee employee = (Employee) employeePicker.getSelectedItem();
            if (employee == null) {
                JOptionPane.showMessageDialog(dialog, "Select an employee.");
                return;
            }
            try {
                Validator.requireText(comments.getText(), "Comments");
                int value = (int) rating.getSelectedItem();
                Validator.validateRating(value);
                Feedback feedback = new Feedback(employee.getUserId(), managerId, comments.getText().trim(), value);
                new FeedbackDAO().add(feedback);
                FileLogger.logInfo("Feedback", "Manager " + managerId
                        + " rated " + employee.getUserId() + " as " + value);
                JOptionPane.showMessageDialog(dialog, "Feedback submitted!");
                dialog.dispose();
                loadFeedbackAsync();
            } catch (InvalidRatingException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Failed: " + e.getMessage());
                FileLogger.logError("Feedback save failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------ edit / delete

    /**
     * Edits the evaluation selected in the table.
     */
    private void editSelectedEvaluation() {
        int row = evalTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an evaluation in the table first.");
            return;
        }
        int id = (int) evalModel.getValueAt(row, 0);
        try {
            Evaluation evaluation = new EvaluationDAO().getById(id);
            if (evaluation == null || evaluation.getManagerId() != managerId) {
                JOptionPane.showMessageDialog(this, "That evaluation is not yours to edit.");
                return;
            }
            showEditEvaluationDialog(evaluation);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    /**
     * Shows the edit dialog for one evaluation.
     *
     * @param evaluation the evaluation to edit
     */
    private void showEditEvaluationDialog(Evaluation evaluation) {
        JDialog dialog = new JDialog(this, "Edit Evaluation #" + evaluation.getEvalId(), true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Integer> communication = ratingCombo();
        JComboBox<Integer> technical = ratingCombo();
        JComboBox<Integer> punctuality = ratingCombo();
        JComboBox<Integer> teamwork = ratingCombo();
        JTextArea comments = new JTextArea(3, 20);
        communication.setSelectedItem(evaluation.getCommunication());
        technical.setSelectedItem(evaluation.getTechnical());
        punctuality.setSelectedItem(evaluation.getPunctuality());
        teamwork.setSelectedItem(evaluation.getTeamwork());
        comments.setText(evaluation.getComments() == null ? "" : evaluation.getComments());

        int y = 0;
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Communication:"), gbc);
        gbc.gridx = 1;
        dialog.add(communication, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Technical:"), gbc);
        gbc.gridx = 1;
        dialog.add(technical, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Punctuality:"), gbc);
        gbc.gridx = 1;
        dialog.add(punctuality, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Teamwork:"), gbc);
        gbc.gridx = 1;
        dialog.add(teamwork, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Comments:"), gbc);
        gbc.gridx = 1;
        dialog.add(new JScrollPane(comments), gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Update");
        save.addActionListener(event -> {
            try {
                Validator.validateRating((int) communication.getSelectedItem());
                Validator.validateRating((int) technical.getSelectedItem());
                Validator.validateRating((int) punctuality.getSelectedItem());
                Validator.validateRating((int) teamwork.getSelectedItem());
                evaluation.setCommunication((int) communication.getSelectedItem());
                evaluation.setTechnical((int) technical.getSelectedItem());
                evaluation.setPunctuality((int) punctuality.getSelectedItem());
                evaluation.setTeamwork((int) teamwork.getSelectedItem());
                evaluation.setComments(comments.getText().trim());
                new EvaluationDAO().update(evaluation);
                FileLogger.logInfo("Evaluation", "Updated eval #" + evaluation.getEvalId());
                JOptionPane.showMessageDialog(dialog, "Updated! Score: "
                        + String.format(Locale.ROOT, "%.2f", evaluation.calculateScore()));
                dialog.dispose();
                loadEvaluationsAsync();
                loadReportAsync();
            } catch (InvalidRatingException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Failed: " + e.getMessage());
                FileLogger.logError("Evaluation update failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Edits the goal selected in the table.
     */
    private void editSelectedGoal() {
        int row = goalTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a goal in the table first.");
            return;
        }
        int id = (int) goalModel.getValueAt(row, 0);
        try {
            Goal goal = new GoalDAO().getById(id);
            if (goal == null || goal.getAssignedBy() != managerId) {
                JOptionPane.showMessageDialog(this, "That goal is not yours to edit.");
                return;
            }
            showEditGoalDialog(goal);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    /**
     * Shows the edit dialog for one goal.
     *
     * @param goal the goal to edit
     */
    private void showEditGoalDialog(Goal goal) {
        JDialog dialog = new JDialog(this, "Edit Goal #" + goal.getGoalId(), true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField title = new JTextField(goal.getTitle(), 20);
        JTextArea description = new JTextArea(goal.getDescription() == null ? "" : goal.getDescription(), 3, 20);
        JTextField dateField = new JTextField(goal.getTargetDate().toString());
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{
                Goal.STATUS_PENDING, Goal.STATUS_IN_PROGRESS, Goal.STATUS_COMPLETED});
        statusCombo.setSelectedItem(goal.getStatus());

        int y = 0;
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        dialog.add(title, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        dialog.add(new JScrollPane(description), gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Target Date:"), gbc);
        gbc.gridx = 1;
        dialog.add(dateField, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        dialog.add(statusCombo, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Update Goal");
        save.addActionListener(event -> {
            try {
                Validator.requireText(title.getText(), "Title");
                goal.setTitle(title.getText().trim());
                goal.setDescription(description.getText().trim());
                goal.setTargetDate(LocalDate.parse(dateField.getText().trim()));
                goal.setStatus((String) statusCombo.getSelectedItem());
                new GoalDAO().update(goal);
                FileLogger.logInfo("Goal", "Updated goal #" + goal.getGoalId());
                JOptionPane.showMessageDialog(dialog, "Goal updated!");
                dialog.dispose();
                loadGoalsAsync();
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (java.time.format.DateTimeParseException e) {
                JOptionPane.showMessageDialog(dialog, "Use the date format YYYY-MM-DD.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Failed: " + e.getMessage());
                FileLogger.logError("Goal update failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Edits the feedback selected in the table.
     */
    private void editSelectedFeedback() {
        int row = feedbackTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a feedback row in the table first.");
            return;
        }
        int id = (int) feedbackModel.getValueAt(row, 0);
        try {
            Feedback feedback = new FeedbackDAO().getById(id);
            if (feedback == null || feedback.getManagerId() != managerId) {
                JOptionPane.showMessageDialog(this, "That feedback is not yours to edit.");
                return;
            }
            showEditFeedbackDialog(feedback);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    /**
     * Shows the edit dialog for one feedback.
     *
     * @param feedback the feedback to edit
     */
    private void showEditFeedbackDialog(Feedback feedback) {
        JDialog dialog = new JDialog(this, "Edit Feedback #" + feedback.getFeedbackId(), true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Integer> rating = ratingCombo();
        rating.setSelectedItem(feedback.getRating());
        JTextArea comments = new JTextArea(feedback.getComments() == null ? "" : feedback.getComments(), 3, 20);

        int y = 0;
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Rating (1-5):"), gbc);
        gbc.gridx = 1;
        dialog.add(rating, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        dialog.add(new JLabel("Comments:"), gbc);
        gbc.gridx = 1;
        dialog.add(new JScrollPane(comments), gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Update Feedback");
        save.addActionListener(event -> {
            try {
                Validator.requireText(comments.getText(), "Comments");
                int value = (int) rating.getSelectedItem();
                Validator.validateRating(value);
                feedback.setRating(value);
                feedback.setComments(comments.getText().trim());
                new FeedbackDAO().updateByManager(feedback, managerId);
                FileLogger.logInfo("Feedback", "Updated feedback #" + feedback.getFeedbackId());
                JOptionPane.showMessageDialog(dialog, "Feedback updated!");
                dialog.dispose();
                loadFeedbackAsync();
            } catch (InvalidRatingException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Failed: " + e.getMessage());
                FileLogger.logError("Feedback update failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Deletes the evaluation selected in the table.
     */
    private void deleteSelectedEvaluation() {
        int row = evalTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an evaluation in the table first.");
            return;
        }
        int id = (int) evalModel.getValueAt(row, 0);
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete evaluation #" + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (new EvaluationDAO().deleteByManager(id, managerId)) {
                JOptionPane.showMessageDialog(this, "Deleted evaluation #" + id);
                loadEvaluationsAsync();
                loadReportAsync();
            } else {
                JOptionPane.showMessageDialog(this, "That evaluation was not found or is not yours.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
            FileLogger.logError("Evaluation delete failed", e);
        }
    }

    /**
     * Exports the manager's evaluations to a CSV file chosen by the user.
     */
    private void exportReport() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File("team_report.csv"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        String path = chooser.getSelectedFile().getAbsolutePath();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new service.ReportService().exportManagerReportCSV(managerId, path);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ManagerDashboard.this, "Exported to " + path);
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    JOptionPane.showMessageDialog(ManagerDashboard.this, "Export failed: " + error.getMessage());
                    FileLogger.logError("Report export failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ManagerDashboard.this, "Export failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Creates a table model whose rows cannot be edited directly.
     *
     * @param columns the column headings
     * @return the read-only model
     */
    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Creates a combo box offering the ratings 1 to 5.
     *
     * @return the combo box
     */
    private static JComboBox<Integer> ratingCombo() {
        JComboBox<Integer> combo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        combo.setSelectedItem(3);
        return combo;
    }

    /**
     * Builds the label shown for an employee, falling back to the id.
     *
     * @param name the employee's name, possibly {@code null}
     * @param id   the employee's id
     * @return the label to display
     */
    private static String displayName(String name, int id) {
        return name == null || name.isBlank() ? "Emp #" + id : name;
    }

    /**
     * Unwraps the original exception from a failed background task.
     *
     * <p>{@code SwingWorker.get()} reports a failure as an
     * {@link ExecutionException}; the real cause (usually a
     * {@link java.sql.SQLException}) is what is worth showing and logging.
     *
     * @param e the wrapper exception
     * @return the original cause, or the wrapper itself when there is none
     */
    private static Exception unwrap(ExecutionException e) {
        Throwable cause = e.getCause();
        return cause instanceof Exception ? (Exception) cause : e;
    }
}
