package ui;

import dao.EvaluationDAO;
import dao.FeedbackDAO;
import dao.GoalDAO;
import dao.UserDAO;
import model.Evaluation;
import model.Feedback;
import model.Goal;
import model.User;
import util.FileLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Manager Dashboard - 5 Tabs wired to real DB via SwingWorker (Multithreading)
 */
public class ManagerDashboard extends JFrame {

    private final String managerName;
    private final int managerId;
    private DefaultTableModel evalModel, goalModel, feedbackModel, teamModel;
    private JLabel statusLabel;

    public ManagerDashboard(String managerName, int managerId) {
        this.managerName = managerName;
        this.managerId = managerId;
        setTitle("Manager Dashboard - " + managerName + " | Performetrics");
        setSize(1050, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(33, 33, 33));
        JLabel welcome = new JLabel("  Manager: " + managerName + "  (ID: " + managerId + ")");
        welcome.setForeground(Color.WHITE);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 15));
        top.add(welcome, BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
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

        statusLabel = new JLabel("  Loading data from database...");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        add(statusLabel, BorderLayout.SOUTH);

        loadAllDataAsync();
    }

    // Backward compatible constructor
    public ManagerDashboard(String managerEmail) {
        this(managerEmail, 1);
    }

    private JPanel createEvaluationPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"Eval ID", "Employee", "Comm", "Tech", "Punct", "Team", "Score", "Date", "Comments"};
        evalModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(evalModel)), BorderLayout.CENTER);
        JPanel btn = new JPanel();
        JButton addBtn = new JButton("Add Evaluation");
        addBtn.addActionListener(e -> showAddEvaluationDialog());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadEvaluationsAsync());
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> deleteSelectedEvaluation());
        btn.add(addBtn); btn.add(refreshBtn); btn.add(deleteBtn);
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createGoalPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"Goal ID", "Title", "Assigned To", "Target Date", "Status", "Personal?"};
        goalModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(goalModel)), BorderLayout.CENTER);
        JPanel btn = new JPanel();
        JButton addBtn = new JButton("Set New Goal");
        addBtn.addActionListener(e -> showAddGoalDialog());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadGoalsAsync());
        btn.add(addBtn); btn.add(refreshBtn);
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createFeedbackPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"Feedback ID", "Employee", "Rating", "Comments", "Date"};
        feedbackModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(feedbackModel)), BorderLayout.CENTER);
        JPanel btn = new JPanel();
        JButton addBtn = new JButton("Give Feedback");
        addBtn.addActionListener(e -> showAddFeedbackDialog());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadFeedbackAsync());
        btn.add(addBtn); btn.add(refreshBtn);
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createReportsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Performance Reports - Live from DB (SwingWorker)", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        p.add(label, BorderLayout.NORTH);
        JPanel chart = new JPanel();
        chart.setBackground(new Color(240, 244, 255));
        chart.setBorder(BorderFactory.createTitledBorder("Team Averages (calculated from evaluations)"));
        chart.setPreferredSize(new Dimension(800, 250));
        chart.setLayout(new BoxLayout(chart, BoxLayout.Y_AXIS));
        JLabel chartLabel = new JLabel("Loading team stats...");
        chartLabel.setName("chartLabel");
        chartLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chartLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        chart.add(Box.createVerticalGlue());
        chart.add(chartLabel);
        chart.add(Box.createVerticalGlue());
        // Store reference via client property
        p.putClientProperty("chartLabel", chartLabel);
        p.add(chart, BorderLayout.CENTER);
        JButton exportBtn = new JButton("Export Team Report to CSV (File I/O)");
        exportBtn.addActionListener(e -> exportReport());
        JPanel south = new JPanel();
        south.add(exportBtn);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createTeamPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"User ID", "Name", "Role", "Department", "Designation", "Email"};
        teamModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(teamModel)), BorderLayout.CENTER);
        JPanel btn = new JPanel();
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadTeamAsync());
        btn.add(refreshBtn);
        btn.add(new JLabel("(Add member via DB or future dialog)"));
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    // --- Async loading via SwingWorker (Multithreading demo) ---

    private void loadAllDataAsync() {
        loadEvaluationsAsync();
        loadGoalsAsync();
        loadFeedbackAsync();
        loadTeamAsync();
        loadReportAsync();
    }

    private void loadEvaluationsAsync() {
        new SwingWorker<List<Evaluation>, Void>() {
            protected List<Evaluation> doInBackground() throws Exception { return new EvaluationDAO().getAll(); }
            protected void done() {
                try {
                    evalModel.setRowCount(0);
                    for (Evaluation e : get()) {
                        evalModel.addRow(new Object[]{e.getEvalId(), e.getEmployeeName() != null ? e.getEmployeeName() : String.valueOf(e.getEmployeeId()),
                                e.getCommunication(), e.getTechnical(), e.getPunctuality(), e.getTeamwork(),
                                String.format("%.2f", e.getOverallScore()), e.getEvalDate(), e.getComments()});
                    }
                    statusLabel.setText("  Evaluations loaded: " + get().size());
                    FileLogger.logInfo("Load", "Manager loaded " + get().size() + " evaluations");
                } catch (Exception ex) { statusLabel.setText("  Evaluations load failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void loadGoalsAsync() {
        new SwingWorker<List<Goal>, Void>() {
            protected List<Goal> doInBackground() throws Exception { return new GoalDAO().getAll(); }
            protected void done() {
                try {
                    goalModel.setRowCount(0);
                    for (Goal g : get()) {
                        goalModel.addRow(new Object[]{g.getGoalId(), g.getTitle(), g.getAssignedTo(), g.getTargetDate(), g.getStatus(), g.isPersonal() ? "Yes" : "No"});
                    }
                } catch (Exception ex) { statusLabel.setText("  Goals load failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void loadFeedbackAsync() {
        new SwingWorker<List<Feedback>, Void>() {
            protected List<Feedback> doInBackground() throws Exception { return new FeedbackDAO().getAll(); }
            protected void done() {
                try {
                    feedbackModel.setRowCount(0);
                    for (Feedback f : get()) {
                        feedbackModel.addRow(new Object[]{f.getFeedbackId(), f.getEmployeeId(), f.getRating(), f.getComments(), f.getGivenOn()});
                    }
                } catch (Exception ex) { statusLabel.setText("  Feedback load failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void loadTeamAsync() {
        new SwingWorker<List<User>, Void>() {
            protected List<User> doInBackground() throws Exception { return new UserDAO().getAll(); }
            protected void done() {
                try {
                    teamModel.setRowCount(0);
                    for (User u : get()) {
                        String dept = "", desg = "";
                        if (u instanceof model.Manager) dept = ((model.Manager) u).getDepartment();
                        else if (u instanceof model.Employee) { dept = ((model.Employee) u).getDepartment(); desg = ((model.Employee) u).getDesignation(); }
                        teamModel.addRow(new Object[]{u.getUserId(), u.getName(), u.getRole(), dept, desg, u.getEmail()});
                    }
                } catch (Exception ex) { statusLabel.setText("  Team load failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void loadReportAsync() {
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                EvaluationDAO dao = new EvaluationDAO();
                java.util.Map<Integer, List<Evaluation>> grouped = dao.getGroupedByEmployee();
                if (grouped.isEmpty()) return "No evaluation data yet.";
                StringBuilder sb = new StringBuilder("<html>");
                for (var entry : grouped.entrySet()) {
                    double avg = entry.getValue().stream().mapToDouble(Evaluation::getOverallScore).average().orElse(0);
                    String name = entry.getValue().get(0).getEmployeeName();
                    if (name == null) name = "Emp #" + entry.getKey();
                    sb.append(String.format("%s: %.2f/5.0 (%d evals) &nbsp;&nbsp;|&nbsp;&nbsp; ", name, avg, entry.getValue().size()));
                }
                sb.append("</html>");
                return sb.toString();
            }
            protected void done() {
                try {
                    String html = get();
                    // Find chartLabel in the reports tab
                    for (Component c : getContentPane().getComponents()) {
                        if (c instanceof JTabbedPane) {
                            JTabbedPane tp = (JTabbedPane) c;
                            for (int i = 0; i < tp.getTabCount(); i++) {
                                if ("Performance Reports".equals(tp.getTitleAt(i))) {
                                    JPanel p = (JPanel) tp.getComponentAt(i);
                                    for (Component inner : p.getComponents()) {
                                        if (inner instanceof JPanel) {
                                            for (Component cc : ((JPanel) inner).getComponents()) {
                                                if (cc instanceof JLabel && "chartLabel".equals(cc.getName())) {
                                                    ((JLabel) cc).setText(html);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // --- Dialogs for adding data ---

    private void showAddEvaluationDialog() {
        JDialog d = new JDialog(this, "Add Evaluation", true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField empField = new JTextField(10);
        JComboBox<Integer> comm = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JComboBox<Integer> tech = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JComboBox<Integer> punct = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JComboBox<Integer> team = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JTextArea comments = new JTextArea(3, 20);
        comm.setSelectedIndex(3); tech.setSelectedIndex(4); punct.setSelectedIndex(3); team.setSelectedIndex(4);
        int y=0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Employee ID:"), gbc); gbc.gridx=1; d.add(empField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Communication (1-5):"), gbc); gbc.gridx=1; d.add(comm, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Technical (1-5):"), gbc); gbc.gridx=1; d.add(tech, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Punctuality (1-5):"), gbc); gbc.gridx=1; d.add(punct, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Teamwork (1-5):"), gbc); gbc.gridx=1; d.add(team, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Comments:"), gbc); gbc.gridx=1; d.add(new JScrollPane(comments), gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            try {
                int empId = Integer.parseInt(empField.getText().trim());
                Evaluation ev = new Evaluation(empId, managerId, (int)comm.getSelectedItem(), (int)tech.getSelectedItem(), (int)punct.getSelectedItem(), (int)team.getSelectedItem(), comments.getText());
                new EvaluationDAO().add(ev);
                FileLogger.logInfo("Evaluation", "Manager " + managerId + " evaluated employee " + empId + " score " + ev.calculateScore());
                JOptionPane.showMessageDialog(d, "Evaluation saved! Score: " + String.format("%.2f", ev.calculateScore()));
                d.dispose();
                loadEvaluationsAsync(); loadReportAsync();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(d, "Enter valid Employee ID"); }
            catch (Exception ex) { JOptionPane.showMessageDialog(d, "Save failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void showAddGoalDialog() {
        JDialog d = new JDialog(this, "Set New Goal", true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField title = new JTextField(20);
        JTextArea desc = new JTextArea(3, 20);
        JTextField empField = new JTextField(10);
        JTextField dateField = new JTextField(10); dateField.setText(LocalDate.now().plusDays(30).toString());
        int y=0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Title:"), gbc); gbc.gridx=1; d.add(title, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Description:"), gbc); gbc.gridx=1; d.add(new JScrollPane(desc), gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Assign to Emp ID:"), gbc); gbc.gridx=1; d.add(empField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Target Date (YYYY-MM-DD):"), gbc); gbc.gridx=1; d.add(dateField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Save Goal");
        save.addActionListener(e -> {
            try {
                if (title.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(d, "Enter title"); return; }
                int empId = Integer.parseInt(empField.getText().trim());
                LocalDate target = LocalDate.parse(dateField.getText().trim());
                util.Validator.validateGoalDeadline(target);
                Goal g = new Goal(title.getText().trim(), desc.getText(), empId, managerId, target, false);
                new GoalDAO().add(g);
                FileLogger.logInfo("Goal", "Goal '" + g.getTitle() + "' assigned to " + empId);
                JOptionPane.showMessageDialog(d, "Goal saved!");
                d.dispose(); loadGoalsAsync();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(d, "Enter valid Employee ID"); }
            catch (exception.GoalDeadlineException ex) { JOptionPane.showMessageDialog(d, ex.getMessage()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(d, "Save failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void showAddFeedbackDialog() {
        JDialog d = new JDialog(this, "Give Feedback", true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField empField = new JTextField(10);
        JComboBox<Integer> rating = new JComboBox<>(new Integer[]{1,2,3,4,5}); rating.setSelectedIndex(4);
        JTextArea comments = new JTextArea(3, 20);
        int y=0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Employee ID:"), gbc); gbc.gridx=1; d.add(empField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Rating (1-5):"), gbc); gbc.gridx=1; d.add(rating, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Comments:"), gbc); gbc.gridx=1; d.add(new JScrollPane(comments), gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Submit Feedback");
        save.addActionListener(e -> {
            try {
                int empId = Integer.parseInt(empField.getText().trim());
                int r = (int) rating.getSelectedItem();
                util.Validator.validateRating(r);
                if (comments.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(d, "Enter comments"); return; }
                Feedback f = new Feedback(empId, managerId, comments.getText(), r);
                new FeedbackDAO().add(f);
                FileLogger.logInfo("Feedback", "Manager " + managerId + " rated " + empId + " as " + r);
                JOptionPane.showMessageDialog(d, "Feedback submitted!");
                d.dispose(); loadFeedbackAsync();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(d, "Enter valid Employee ID"); }
            catch (exception.InvalidRatingException ex) { JOptionPane.showMessageDialog(d, ex.getMessage()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(d, "Failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void deleteSelectedEvaluation() {
        JTable table = null;
        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JTabbedPane) {
                JPanel p = (JPanel) ((JTabbedPane) c).getComponentAt(0);
                for (Component cc : p.getComponents()) if (cc instanceof JScrollPane) table = (JTable) ((JScrollPane) cc).getViewport().getView();
            }
        }
        // Simpler: get first tab's table via evalModel selected row
        // Use evalModel directly with JOptionPane input
        String input = JOptionPane.showInputDialog(this, "Enter Eval ID to delete:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int id = Integer.parseInt(input.trim());
            if (new EvaluationDAO().delete(id)) {
                JOptionPane.showMessageDialog(this, "Deleted Eval #" + id);
                loadEvaluationsAsync(); loadReportAsync();
            } else JOptionPane.showMessageDialog(this, "Not found");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage()); }
    }

    private void exportReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("team_report.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception {
                    new dao.EvaluationDAO().getAll(); // test
                    new service.ReportService().exportTeamReportCSV(chooser.getSelectedFile().getAbsolutePath());
                    return null;
                }
                protected void done() {
                    try { get(); JOptionPane.showMessageDialog(ManagerDashboard.this, "Exported to " + chooser.getSelectedFile().getAbsolutePath()); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(ManagerDashboard.this, "Export failed: " + ex.getMessage()); }
                }
            }.execute();
        }
    }
}
