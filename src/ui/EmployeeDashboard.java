package ui;

import dao.EvaluationDAO;
import dao.FeedbackDAO;
import dao.GoalDAO;
import model.Evaluation;
import model.Feedback;
import model.Goal;
import util.FileLogger;
import util.ReportExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Employee Dashboard - 4 tabs wired to real DB
 */
public class EmployeeDashboard extends JFrame {

    private final String employeeName;
    private final int employeeId;
    private DefaultTableModel trackingModel, feedbackModel, goalsModel;
    private JLabel avgLabel, chartLabel;

    public EmployeeDashboard(String employeeName, int employeeId) {
        this.employeeName = employeeName;
        this.employeeId = employeeId;
        setTitle("Employee Dashboard - " + employeeName + " (ID: " + employeeId + ") | Performetrics");
        setSize(1050, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(0, 105, 92));
        JLabel welcome = new JLabel("  Employee: " + employeeName);
        welcome.setForeground(Color.WHITE);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 15));
        top.add(welcome, BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
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

    public EmployeeDashboard(String employeeEmail) { this(employeeEmail, 2); }

    private JPanel createTrackingPanel() {
        JPanel p = new JPanel(new BorderLayout());
        avgLabel = new JLabel("Average Score: loading...", SwingConstants.CENTER);
        avgLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(avgLabel, BorderLayout.NORTH);
        JPanel chart = new JPanel();
        chart.setBackground(new Color(232, 245, 233));
        chart.setBorder(BorderFactory.createTitledBorder("Your Score Trend (Line Chart from DB)"));
        chart.setPreferredSize(new Dimension(800, 150));
        chartLabel = new JLabel("Loading...");
        chartLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chart.add(chartLabel);
        p.add(chart, BorderLayout.CENTER);
        String[] cols = {"Eval ID", "Date", "Comm", "Tech", "Punct", "Team", "Score", "Comments"};
        trackingModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(trackingModel)), BorderLayout.SOUTH);
        return p;
    }

    private JPanel createFeedbackPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("  Feedback Received from Managers", SwingConstants.LEFT), BorderLayout.NORTH);
        String[] cols = {"Feedback ID", "Manager ID", "Rating", "Comments", "Date"};
        feedbackModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(feedbackModel)), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadFeedbackAsync());
        JPanel south = new JPanel(); south.add(refresh);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createGoalsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"Goal ID", "Title", "Target Date", "Status", "Personal?"};
        goalsModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(goalsModel)), BorderLayout.CENTER);
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Set Personal Goal");
        addBtn.addActionListener(e -> showAddPersonalGoalDialog());
        JButton completeBtn = new JButton("Mark Complete");
        completeBtn.addActionListener(e -> markGoalComplete());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadGoalsAsync());
        btnPanel.add(addBtn); btnPanel.add(completeBtn); btnPanel.add(refresh);
        p.add(btnPanel, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createReportsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Personal Performance Report", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        p.add(title, BorderLayout.NORTH);
        JPanel chart = new JPanel();
        chart.setBackground(new Color(255, 243, 224));
        chart.setBorder(BorderFactory.createTitledBorder("Your Performance Breakdown"));
        chart.setPreferredSize(new Dimension(800, 250));
        JLabel reportLabel = new JLabel("Loading report...");
        reportLabel.setName("reportLabel");
        chart.add(reportLabel);
        p.add(chart, BorderLayout.CENTER);
        JButton export = new JButton("Export My Evaluations to CSV (File I/O)");
        export.addActionListener(e -> exportMyReport());
        JPanel south = new JPanel(); south.add(export);
        p.add(south, BorderLayout.SOUTH);
        p.putClientProperty("reportLabel", reportLabel);
        return p;
    }

    private void loadAllAsync(JLabel status) {
        loadTrackingAsync();
        loadFeedbackAsync();
        loadGoalsAsync();
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                EvaluationDAO dao = new EvaluationDAO();
                List<Evaluation> evals = dao.getByEmployeeId(employeeId);
                if (evals.isEmpty()) return "No evaluations yet.";
                double avg = evals.stream().mapToDouble(Evaluation::getOverallScore).average().orElse(0);
                int cAvg = (int) evals.stream().mapToInt(Evaluation::getCommunication).average().orElse(0);
                int tAvg = (int) evals.stream().mapToInt(Evaluation::getTechnical).average().orElse(0);
                return String.format("Avg: %.2f | Comm: %d | Tech: %d | Total evals: %d", avg, cAvg, tAvg, evals.size());
            }
            protected void done() {
                try { status.setText("  " + get()); } catch (Exception ex) { status.setText("  Load error: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void loadTrackingAsync() {
        new SwingWorker<List<Evaluation>, Void>() {
            protected List<Evaluation> doInBackground() throws Exception { return new EvaluationDAO().getByEmployeeId(employeeId); }
            protected void done() {
                try {
                    List<Evaluation> list = get();
                    trackingModel.setRowCount(0);
                    for (Evaluation e : list) {
                        trackingModel.addRow(new Object[]{e.getEvalId(), e.getEvalDate(), e.getCommunication(), e.getTechnical(), e.getPunctuality(), e.getTeamwork(), String.format("%.2f", e.getOverallScore()), e.getComments()});
                    }
                    if (!list.isEmpty()) {
                        double avg = list.stream().mapToDouble(Evaluation::getOverallScore).average().orElse(0);
                        avgLabel.setText(String.format("Average Score: %.2f / 5.0  (%d evaluations)", avg, list.size()));
                        StringBuilder sb = new StringBuilder("<html>");
                        for (Evaluation e : list) sb.append(e.getEvalDate()).append(": ").append(String.format("%.1f", e.getOverallScore())).append(" &nbsp; ");
                        sb.append("</html>");
                        chartLabel.setText(sb.toString());
                    } else {
                        avgLabel.setText("No evaluations yet for you.");
                        chartLabel.setText("No data - ask your manager for an evaluation.");
                    }
                } catch (Exception ex) { avgLabel.setText("Load failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void loadFeedbackAsync() {
        new SwingWorker<List<Feedback>, Void>() {
            protected List<Feedback> doInBackground() throws Exception { return new FeedbackDAO().getByEmployee(employeeId); }
            protected void done() {
                try {
                    feedbackModel.setRowCount(0);
                    for (Feedback f : get()) feedbackModel.addRow(new Object[]{f.getFeedbackId(), f.getManagerId(), f.getRating(), f.getComments(), f.getGivenOn()});
                } catch (Exception ex) {}
            }
        }.execute();
    }

    private void loadGoalsAsync() {
        new SwingWorker<List<Goal>, Void>() {
            protected List<Goal> doInBackground() throws Exception { return new GoalDAO().getByEmployee(employeeId); }
            protected void done() {
                try {
                    goalsModel.setRowCount(0);
                    for (Goal g : get()) goalsModel.addRow(new Object[]{g.getGoalId(), g.getTitle(), g.getTargetDate(), g.getStatus(), g.isPersonal() ? "Yes" : "No"});
                } catch (Exception ex) {}
            }
        }.execute();
    }

    private void showAddPersonalGoalDialog() {
        JDialog d = new JDialog(this, "Set Personal Goal", true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField title = new JTextField(20);
        JTextArea desc = new JTextArea(3, 20);
        JTextField dateField = new JTextField(10); dateField.setText(LocalDate.now().plusDays(30).toString());
        int y=0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Title:"), gbc); gbc.gridx=1; d.add(title, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Description:"), gbc); gbc.gridx=1; d.add(new JScrollPane(desc), gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Target Date (YYYY-MM-DD):"), gbc); gbc.gridx=1; d.add(dateField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Save Personal Goal");
        save.addActionListener(e -> {
            try {
                if (title.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(d, "Enter title"); return; }
                LocalDate target = LocalDate.parse(dateField.getText().trim());
                util.Validator.validateGoalDeadline(target);
                Goal g = new Goal(title.getText().trim(), desc.getText(), employeeId, employeeId, target, true);
                new GoalDAO().add(g);
                FileLogger.logInfo("PersonalGoal", employeeName + " set goal: " + g.getTitle());
                JOptionPane.showMessageDialog(d, "Personal goal saved!");
                d.dispose(); loadGoalsAsync();
            } catch (exception.GoalDeadlineException ex) { JOptionPane.showMessageDialog(d, ex.getMessage()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(d, "Failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void markGoalComplete() {
        String input = JOptionPane.showInputDialog(this, "Enter Goal ID to mark Completed:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int id = Integer.parseInt(input.trim());
            if (new GoalDAO().updateStatus(id, "Completed")) {
                JOptionPane.showMessageDialog(this, "Goal #" + id + " marked Completed!");
                loadGoalsAsync();
            } else JOptionPane.showMessageDialog(this, "Goal not found or not yours.");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage()); }
    }

    private void exportMyReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("my_evaluations_" + employeeId + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception {
                    List<Evaluation> list = new EvaluationDAO().getByEmployeeId(employeeId);
                    ReportExporter.exportEvaluationsToCSV(list, chooser.getSelectedFile().getAbsolutePath());
                    return null;
                }
                protected void done() {
                    try { get(); JOptionPane.showMessageDialog(EmployeeDashboard.this, "Exported to " + chooser.getSelectedFile().getAbsolutePath() + "\n(File I/O demo)"); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(EmployeeDashboard.this, "Export failed: " + ex.getMessage()); }
                }
            }.execute();
        }
    }
}
