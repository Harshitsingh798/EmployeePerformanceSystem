package ui;

import dao.EvaluationDAO;
import dao.FeedbackDAO;
import dao.GoalDAO;
import exception.GoalDeadlineException;
import model.Evaluation;
import model.Feedback;
import model.Goal;
import util.FileLogger;
import util.ReportExporter;
import util.Validator;
import ui.components.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The employee's workspace.
 *
 * <p>Four tabs - performance tracking, feedback history, personal goals and
 * performance reports - all backed by the live database.
 *
 * <p>The employee only ever sees their own rows: every query is filtered by
 * the signed-in employee's id, and every write is re-checked against that id
 * in the database so that guessing an id cannot reach another employee's data.
 */
public final class EmployeeDashboard extends JFrame {

    /** Serialisation version - required for a {@link java.io.Serializable} subclass. */
    private static final long serialVersionUID = 1L;

    private final String employeeName;
    private final int employeeId;

    private final DefaultTableModel trackingModel;
    private final DefaultTableModel feedbackModel;
    private final DefaultTableModel goalsModel;
    private final JTable trackingTable;
    private final JTable feedbackTable;
    private final JTable goalsTable;

    private final JLabel averageLabel = new JLabel("Average Score: loading...", SwingConstants.CENTER);

    /** Holds the score trend chart on the tracking tab. */
    private final JPanel trackingChartSlot = new JPanel(new BorderLayout());

    /** Holds the criteria pie chart on the reports tab. */
    private final JPanel reportsPieSlot = new JPanel(new BorderLayout());

    /** Holds the criteria bar chart on the reports tab. */
    private final JPanel reportsBarSlot = new JPanel(new BorderLayout());

    /** Holds the goal status pie chart on the goals tab. */
    private final JPanel goalStatusSlot = new JPanel(new BorderLayout());

    /**
     * Builds the dashboard.
     *
     * @param employeeName the name shown in the header
     * @param employeeId   the signed-in employee's user id
     */
    public EmployeeDashboard(String employeeName, int employeeId) {
        this.employeeName = employeeName;
        this.employeeId = employeeId;

        setTitle("Employee Dashboard - " + employeeName + " (ID: " + employeeId + ") | Performetrics");
        setSize(1080, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        trackingModel = readOnlyModel("Eval ID", "Date", "Comm", "Tech", "Punct", "Team", "Score", "Comments");
        feedbackModel = readOnlyModel("Feedback ID", "Manager ID", "Rating", "Comments", "Date");
        goalsModel = readOnlyModel("Goal ID", "Title", "Type", "Target Date", "Status");

        trackingTable = new JTable(trackingModel);
        feedbackTable = new JTable(feedbackModel);
        goalsTable = new JTable(goalsModel);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(0, 105, 92));
        JLabel welcome = new JLabel("  Employee: " + employeeName);
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
        tabs.addTab("Performance Tracking", createTrackingPanel());
        tabs.addTab("Feedback History", createFeedbackPanel());
        tabs.addTab("Personal Goals", createGoalsPanel());
        tabs.addTab("Performance Reports", createReportsPanel());
        add(tabs, BorderLayout.CENTER);

        JLabel status = new JLabel("  Loading your data...");
        status.setBorder(BorderFactory.createEtchedBorder());
        add(status, BorderLayout.SOUTH);

        loadAllAsync(status);
    }

    // ------------------------------------------------------------------ panels

    /**
     * Builds the performance tracking tab: summary, trend chart and table.
     */
    private JPanel createTrackingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        averageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(averageLabel, BorderLayout.NORTH);

        trackingChartSlot.setBorder(BorderFactory.createTitledBorder("Your Score Trend (Line Chart)"));
        panel.add(trackingChartSlot, BorderLayout.CENTER);

        // Cap the height of the history table, so the chart above it gets
        // enough room to actually show the trend.
        JScrollPane history = new JScrollPane(trackingTable);
        history.setPreferredSize(new Dimension(0, 190));
        panel.add(history, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Builds the feedback history tab.
     */
    private JPanel createFeedbackPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("  Feedback Received from Managers", SwingConstants.LEFT), BorderLayout.NORTH);
        panel.add(new JScrollPane(feedbackTable), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> loadFeedbackAsync());
        JPanel south = new JPanel();
        south.add(refresh);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Builds the personal goals tab: table plus a status pie chart.
     */
    private JPanel createGoalsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(goalsTable), BorderLayout.CENTER);

        goalStatusSlot.setBorder(BorderFactory.createTitledBorder("Goal Status Distribution"));
        panel.add(goalStatusSlot, BorderLayout.EAST);

        JButton add = new JButton("Set Personal Goal");
        JButton complete = new JButton("Mark Complete");
        JButton edit = new JButton("Edit Selected");
        JButton delete = new JButton("Delete Selected");
        JButton refresh = new JButton("Refresh");
        add.addActionListener(event -> showAddPersonalGoalDialog());
        complete.addActionListener(event -> markGoalComplete());
        edit.addActionListener(event -> editSelectedGoal());
        delete.addActionListener(event -> deleteSelectedGoal());
        refresh.addActionListener(event -> loadGoalsAsync());

        JPanel buttons = new JPanel();
        buttons.add(add);
        buttons.add(complete);
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(refresh);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Builds the performance reports tab with the two criteria charts.
     */
    private JPanel createReportsPanel() {
        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 10));
        charts.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reportsBarSlot.setBorder(BorderFactory.createTitledBorder("Your Criteria Averages (Bar)"));
        reportsPieSlot.setBorder(BorderFactory.createTitledBorder("Criteria Breakdown (Pie)"));
        charts.add(reportsBarSlot);
        charts.add(reportsPieSlot);

        JButton export = new JButton("Export My Evaluations to CSV");
        export.addActionListener(event -> exportMyReport());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(charts, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.add(export);
        wrapper.add(south, BorderLayout.SOUTH);
        return wrapper;
    }

    // ------------------------------------------------------------- async loading

    /**
     * Starts every background load.
     *
     * @param status the label that shows the load result
     */
    private void loadAllAsync(JLabel status) {
        loadTrackingAsync();
        loadFeedbackAsync();
        loadGoalsAsync();
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                List<Evaluation> evaluations = new EvaluationDAO().getByEmployeeId(employeeId);
                if (evaluations.isEmpty()) {
                    return "No evaluations yet.";
                }
                double average = evaluations.stream().mapToDouble(Evaluation::calculateScore).average().orElse(0);
                int communication = (int) evaluations.stream().mapToInt(Evaluation::getCommunication).average().orElse(0);
                int technical = (int) evaluations.stream().mapToInt(Evaluation::getTechnical).average().orElse(0);
                return String.format(Locale.ROOT, "Avg: %.2f | Comm: %d | Tech: %d | Total evals: %d",
                        average, communication, technical, evaluations.size());
            }

            @Override
            protected void done() {
                try {
                    status.setText("  " + get());
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    status.setText("  Load error: " + error.getMessage());
                    FileLogger.logError("Employee summary load failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    status.setText("  Load error: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Loads this employee's evaluations and refreshes the trend chart.
     */
    private void loadTrackingAsync() {
        new SwingWorker<List<Evaluation>, Void>() {
            @Override
            protected List<Evaluation> doInBackground() throws Exception {
                return new EvaluationDAO().getByEmployeeId(employeeId);
            }

            @Override
            protected void done() {
                try {
                    List<Evaluation> evaluations = get();
                    trackingModel.setRowCount(0);
                    for (Evaluation evaluation : evaluations) {
                        trackingModel.addRow(new Object[]{
                                evaluation.getEvalId(),
                                evaluation.getEvalDate(),
                                evaluation.getCommunication(),
                                evaluation.getTechnical(),
                                evaluation.getPunctuality(),
                                evaluation.getTeamwork(),
                                String.format(Locale.ROOT, "%.2f", evaluation.calculateScore()),
                                evaluation.getComments()});
                    }
                    trackingChartSlot.removeAll();
                    if (evaluations.isEmpty()) {
                        averageLabel.setText("No evaluations yet for you.");
                        trackingChartSlot.add(new JLabel("No data - ask your manager for an evaluation.",
                                SwingConstants.CENTER));
                    } else {
                        double average = evaluations.stream().mapToDouble(Evaluation::calculateScore).average().orElse(0);
                        averageLabel.setText(String.format(Locale.ROOT,
                                "Average Score: %.2f / 5.0  (%d evaluations)", average, evaluations.size()));
                        List<LocalDate> dates = new ArrayList<>();
                        List<Double> scores = new ArrayList<>();
                        for (Evaluation evaluation : evaluations) {
                            dates.add(evaluation.getEvalDate());
                            scores.add(evaluation.calculateScore());
                        }
                        trackingChartSlot.add(ChartPanel.line("Your Score Over Time", dates, scores),
                                BorderLayout.CENTER);
                    }
                    trackingChartSlot.revalidate();
                    trackingChartSlot.repaint();
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    averageLabel.setText("Load failed: " + error.getMessage());
                    FileLogger.logError("Tracking load failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    averageLabel.setText("Load failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Loads the feedback addressed to this employee.
     */
    private void loadFeedbackAsync() {
        new SwingWorker<List<Feedback>, Void>() {
            @Override
            protected List<Feedback> doInBackground() throws Exception {
                return new FeedbackDAO().getByEmployee(employeeId);
            }

            @Override
            protected void done() {
                try {
                    List<Feedback> feedbackList = get();
                    feedbackModel.setRowCount(0);
                    for (Feedback feedback : feedbackList) {
                        feedbackModel.addRow(new Object[]{
                                feedback.getFeedbackId(),
                                feedback.getManagerId(),
                                feedback.getRating(),
                                feedback.getComments(),
                                feedback.getGivenOn()});
                    }
                } catch (ExecutionException e) {
                    FileLogger.logError("Feedback load failed", unwrap(e));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    FileLogger.logError("Feedback load failed", e);
                }
            }
        }.execute();
    }

    /**
     * Loads this employee's goals and refreshes the status pie chart.
     */
    private void loadGoalsAsync() {
        new SwingWorker<List<Goal>, Void>() {
            @Override
            protected List<Goal> doInBackground() throws Exception {
                return new GoalDAO().getByEmployee(employeeId);
            }

            @Override
            protected void done() {
                try {
                    List<Goal> goals = get();
                    goalsModel.setRowCount(0);
                    for (Goal goal : goals) {
                        goalsModel.addRow(new Object[]{
                                goal.getGoalId(),
                                goal.getTitle(),
                                goal.isPersonal() ? "Personal" : "From Manager",
                                goal.getTargetDate(),
                                goal.getStatus()});
                    }
                    Map<String, Integer> statusCount = new LinkedHashMap<>();
                    for (Goal goal : goals) {
                        statusCount.merge(goal.getStatus(), 1, Integer::sum);
                    }
                    Map<String, Double> pieData = new LinkedHashMap<>();
                    for (Map.Entry<String, Integer> entry : statusCount.entrySet()) {
                        pieData.put(entry.getKey(), entry.getValue().doubleValue());
                    }
                    goalStatusSlot.removeAll();
                    goalStatusSlot.add(ChartPanel.pie("Goal Status Distribution", pieData), BorderLayout.CENTER);
                    goalStatusSlot.revalidate();
                    goalStatusSlot.repaint();
                    loadReportAsync();
                } catch (ExecutionException e) {
                    FileLogger.logError("Goal load failed", unwrap(e));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    FileLogger.logError("Goal load failed", e);
                }
            }
        }.execute();
    }

    /**
     * Loads the criteria charts for the reports tab.
     */
    private void loadReportAsync() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                List<Evaluation> evaluations = new EvaluationDAO().getByEmployeeId(employeeId);
                Map<String, Double> criteria = new LinkedHashMap<>();
                if (!evaluations.isEmpty()) {
                    criteria.put("Communication", evaluations.stream()
                            .mapToInt(Evaluation::getCommunication).average().orElse(0));
                    criteria.put("Technical", evaluations.stream()
                            .mapToInt(Evaluation::getTechnical).average().orElse(0));
                    criteria.put("Punctuality", evaluations.stream()
                            .mapToInt(Evaluation::getPunctuality).average().orElse(0));
                    criteria.put("Teamwork", evaluations.stream()
                            .mapToInt(Evaluation::getTeamwork).average().orElse(0));
                }
                return new Object[]{criteria};
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    @SuppressWarnings("unchecked")
                    Map<String, Double> criteria = (Map<String, Double>) result[0];
                    reportsPieSlot.removeAll();
                    reportsPieSlot.add(ChartPanel.pie("Criteria Breakdown", criteria), BorderLayout.CENTER);
                    reportsBarSlot.removeAll();
                    reportsBarSlot.add(ChartPanel.bar("Criteria Averages", "Rating", criteria), BorderLayout.CENTER);
                    reportsPieSlot.revalidate();
                    reportsPieSlot.repaint();
                    reportsBarSlot.revalidate();
                    reportsBarSlot.repaint();
                } catch (ExecutionException e) {
                    FileLogger.logError("Report load failed", unwrap(e));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    FileLogger.logError("Report load failed", e);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------ dialogs

    /**
     * Opens the dialog that creates a personal goal.
     */
    private void showAddPersonalGoalDialog() {
        JDialog dialog = new JDialog(this, "Set Personal Goal", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField title = new JTextField(20);
        JTextArea description = new JTextArea(3, 20);
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
        dialog.add(new JLabel("Target Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dialog.add(dateField, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;

        JButton save = new JButton("Save Personal Goal");
        save.addActionListener(event -> {
            try {
                Validator.requireText(title.getText(), "Title");
                LocalDate target = LocalDate.parse(dateField.getText().trim());
                Validator.validateGoalDeadline(target);
                Goal goal = new Goal(title.getText().trim(), description.getText().trim(),
                        employeeId, employeeId, target, true);
                new GoalDAO().add(goal);
                FileLogger.logInfo("PersonalGoal", employeeName + " set goal: " + goal.getTitle());
                JOptionPane.showMessageDialog(dialog, "Personal goal saved!");
                dialog.dispose();
                loadGoalsAsync();
            } catch (GoalDeadlineException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage());
            } catch (java.time.format.DateTimeParseException e) {
                JOptionPane.showMessageDialog(dialog, "Use the date format YYYY-MM-DD.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Failed: " + e.getMessage());
                FileLogger.logError("Personal goal save failed", e);
            }
        });
        dialog.add(save, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Marks the selected goal as completed.
     */
    private void markGoalComplete() {
        int row = goalsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a goal in the table first.");
            return;
        }
        int id = (int) goalsModel.getValueAt(row, 0);
        try {
            if (new GoalDAO().updateStatus(id, Goal.STATUS_COMPLETED, employeeId)) {
                JOptionPane.showMessageDialog(this, "Goal #" + id + " marked completed!");
                loadGoalsAsync();
            } else {
                JOptionPane.showMessageDialog(this, "Goal not found or not yours.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed: " + e.getMessage());
            FileLogger.logError("Goal status update failed", e);
        }
    }

    /**
     * Edits the selected goal.
     */
    private void editSelectedGoal() {
        int row = goalsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a goal in the table first.");
            return;
        }
        int id = (int) goalsModel.getValueAt(row, 0);
        try {
            Goal goal = new GoalDAO().getOwnedGoal(id, employeeId);
            if (goal == null) {
                JOptionPane.showMessageDialog(this, "That goal was not found or is not yours.");
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
                new GoalDAO().updateOwnedGoal(goal, employeeId);
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
     * Deletes the selected goal.
     */
    private void deleteSelectedGoal() {
        int row = goalsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a goal in the table first.");
            return;
        }
        int id = (int) goalsModel.getValueAt(row, 0);
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete goal #" + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (new GoalDAO().deleteOwnedGoal(id, employeeId)) {
                JOptionPane.showMessageDialog(this, "Deleted goal #" + id);
                loadGoalsAsync();
            } else {
                JOptionPane.showMessageDialog(this, "Goal not found or not yours.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed: " + e.getMessage());
            FileLogger.logError("Goal delete failed", e);
        }
    }

    /**
     * Exports this employee's evaluations to a CSV file chosen by the user.
     */
    private void exportMyReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("my_evaluations_" + employeeId + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String path = chooser.getSelectedFile().getAbsolutePath();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Evaluation> evaluations = new EvaluationDAO().getByEmployeeId(employeeId);
                ReportExporter.exportEvaluationsToCSV(evaluations, path);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(EmployeeDashboard.this, "Exported to " + path);
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    JOptionPane.showMessageDialog(EmployeeDashboard.this, "Export failed: " + error.getMessage());
                    FileLogger.logError("Report export failed", error);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EmployeeDashboard.this, "Export failed: " + e.getMessage());
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
