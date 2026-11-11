package ui;

import dao.EvaluationDAO;
import dao.FeedbackDAO;
import dao.GoalDAO;
import dao.UserDAO;
import dao.TeamDAO;
import model.Evaluation;
import model.Feedback;
import model.Goal;
import model.User;
import util.FileLogger;
import util.Icons;
import ui.components.ChartPanel;

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
    private JPanel reportBarSlot, reportPieSlot;
    private JTable teamTable;

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
        JPanel top = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField(20);
        searchField.setToolTipText("Search by Employee Name or Comments");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterEvaluations(searchField.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterEvaluations(searchField.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        top.add(new JLabel("Search: "), BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH);
        String[] cols = {"Eval ID", "Employee", "Comm", "Tech", "Punct", "Team", "Score", "Date", "Comments"};
        evalModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        p.add(new JScrollPane(new JTable(evalModel)), BorderLayout.CENTER);
        JPanel btn = new JPanel();
        JButton addBtn = new JButton("Add Evaluation");
        addBtn.addActionListener(e -> showAddEvaluationDialog());
        JButton editBtn = new JButton("Edit Selected");
        editBtn.addActionListener(e -> editSelectedEvaluation());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadEvaluationsAsync());
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> deleteSelectedEvaluation());
        util.Icons.styleButton(addBtn, "Add", new Color(41, 98, 255), Color.WHITE);
        util.Icons.styleButton(editBtn, "Edit", new Color(255, 152, 0), Color.WHITE);
        util.Icons.styleButton(deleteBtn, "Delete", new Color(220, 53, 69), Color.WHITE);
        util.Icons.styleButton(refreshBtn, "Refresh", new Color(40, 167, 69), Color.WHITE);
        btn.add(addBtn); btn.add(editBtn); btn.add(refreshBtn); btn.add(deleteBtn);
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    private void filterEvaluations(String text) {
        if (text.trim().isEmpty()) {
            loadEvaluationsAsync();
            return;
        }
        new SwingWorker<List<Evaluation>, Void>() {
            protected List<Evaluation> doInBackground() throws Exception {
                List<Evaluation> all = new EvaluationDAO().getAll();
                List<Evaluation> filtered = new java.util.ArrayList<>();
                String query = text.toLowerCase();
                for (Evaluation e : all) {
                    if ((e.getEmployeeName() != null && e.getEmployeeName().toLowerCase().contains(query)) ||
                        (e.getComments() != null && e.getComments().toLowerCase().contains(query))) {
                        filtered.add(e);
                    }
                }
                return filtered;
            }
            protected void done() {
                try {
                    evalModel.setRowCount(0);
                    for (Evaluation e : get()) {
                        evalModel.addRow(new Object[]{e.getEvalId(), e.getEmployeeName() != null ? e.getEmployeeName() : String.valueOf(e.getEmployeeId()),
                                e.getCommunication(), e.getTechnical(), e.getPunctuality(), e.getTeamwork(),
                                String.format("%.2f", e.getOverallScore()), e.getEvalDate(), e.getComments()});
                    }
                    statusLabel.setText("  Filtered: " + get().size() + " evaluations");
                } catch (Exception ex) { statusLabel.setText("  Filter failed: " + ex.getMessage()); }
            }
        }.execute();
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
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> editSelectedGoal());
        btn.add(addBtn); btn.add(editBtn); btn.add(refreshBtn);
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
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> editSelectedFeedback());
        btn.add(addBtn); btn.add(editBtn); btn.add(refreshBtn);
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createReportsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reportBarSlot = new JPanel(new BorderLayout());
        reportBarSlot.setBorder(BorderFactory.createTitledBorder("Team Average Scores (Bar)"));
        reportPieSlot = new JPanel(new BorderLayout());
        reportPieSlot.setBorder(BorderFactory.createTitledBorder("Criteria Breakdown (Pie)"));
        p.add(reportBarSlot); p.add(reportPieSlot);
        JButton exportBtn = new JButton("Export Team Report to CSV (File I/O)");
        exportBtn.addActionListener(e -> exportReport());
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(p, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.add(exportBtn);
        wrap.add(south, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel createTeamPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"User ID", "Name", "Role", "Department", "Designation", "Email"};
        teamModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        teamTable = new JTable(teamModel);
        p.add(new JScrollPane(teamTable), BorderLayout.CENTER);
        JPanel btn = new JPanel();
        JButton addMemberBtn = new JButton("Add Member to Team");
        addMemberBtn.addActionListener(e -> showAddMemberDialog(false));
        JButton assignRoleBtn = new JButton("Assign Role");
        assignRoleBtn.addActionListener(e -> showAddMemberDialog(true));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadTeamAsync());
        btn.add(addMemberBtn); btn.add(assignRoleBtn); btn.add(refreshBtn);
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
        new SwingWorker<Object[], Void>() {
            protected Object[] doInBackground() throws Exception {
                EvaluationDAO dao = new EvaluationDAO();
                java.util.Map<Integer, List<Evaluation>> grouped = dao.getGroupedByEmployee();
                java.util.Map<String, Double> barData = new java.util.LinkedHashMap<>();
                java.util.List<Evaluation> all = dao.getAll();
                double cSum = 0, tSum = 0, pSum = 0, wSum = 0;
                for (var entry : grouped.entrySet()) {
                    double avg = entry.getValue().stream().mapToDouble(Evaluation::getOverallScore).average().orElse(0);
                    String name = entry.getValue().get(0).getEmployeeName();
                    if (name == null) name = "Emp #" + entry.getKey();
                    barData.put(name, avg);
                }
                for (Evaluation e : all) { cSum += e.getCommunication(); tSum += e.getTechnical(); pSum += e.getPunctuality(); wSum += e.getTeamwork(); }
                java.util.Map<String, Double> pieData = new java.util.LinkedHashMap<>();
                if (!all.isEmpty()) {
                    int n = all.size();
                    pieData.put("Communication", cSum / n);
                    pieData.put("Technical", tSum / n);
                    pieData.put("Punctuality", pSum / n);
                    pieData.put("Teamwork", wSum / n);
                }
                return new Object[]{barData, pieData};
            }
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] res = get();
                    reportBarSlot.removeAll();
                    reportBarSlot.add(ChartPanel.bar("Avg Score per Employee", "Score", (java.util.Map<String, Double>) res[0]), BorderLayout.CENTER);
                    reportPieSlot.removeAll();
                    reportPieSlot.add(ChartPanel.pie("Criteria Averages", (java.util.Map<String, Double>) res[1]), BorderLayout.CENTER);
                    reportBarSlot.revalidate(); reportBarSlot.repaint();
                    reportPieSlot.revalidate(); reportPieSlot.repaint();
                } catch (Exception ex) { statusLabel.setText("  Report load failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void showAddMemberDialog(boolean updateOnly) {
        JDialog d = new JDialog(this, updateOnly ? "Update Member Role" : "Add Member to Team", true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField teamField = new JTextField(10); teamField.setText("1");
        JTextField empField = new JTextField(10);
        JTextField roleField = new JTextField(15); roleField.setText(updateOnly ? "Lead Developer" : "Developer");
        int y = 0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Team ID:"), gbc); gbc.gridx=1; d.add(teamField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Employee ID:"), gbc); gbc.gridx=1; d.add(empField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Role:"), gbc); gbc.gridx=1; d.add(roleField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            try {
                int teamId = Integer.parseInt(teamField.getText().trim());
                int empId = Integer.parseInt(empField.getText().trim());
                String role = roleField.getText().trim();
                TeamDAO dao = new TeamDAO();
                boolean ok;
                if (updateOnly) {
                    dao.removeMember(teamId, empId);
                    ok = dao.addMember(teamId, empId, role);
                } else {
                    ok = dao.addMember(teamId, empId, role);
                }
                if (ok) {
                    FileLogger.logInfo("Team", "Member " + empId + " role '" + role + "' in team " + teamId);
                    JOptionPane.showMessageDialog(d, "Saved!");
                    d.dispose(); loadTeamAsync();
                } else JOptionPane.showMessageDialog(d, "Already a member or team not found");
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(d, "Enter valid IDs"); }
            catch (Exception ex) { JOptionPane.showMessageDialog(d, "Failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
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

    private void editSelectedEvaluation() {
        int row = evalModel.getRowCount();
        if (row == 0) { JOptionPane.showMessageDialog(this, "No evaluations to edit"); return; }
        String input = JOptionPane.showInputDialog(this, "Enter Eval ID to edit:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int id = Integer.parseInt(input.trim());
            Evaluation e = new EvaluationDAO().getById(id);
            if (e == null) { JOptionPane.showMessageDialog(this, "Not found"); return; }
            showEditEvaluationDialog(e);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
    }

    private void showEditEvaluationDialog(Evaluation e) {
        JDialog d = new JDialog(this, "Edit Evaluation #" + e.getEvalId(), true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<Integer> comm = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JComboBox<Integer> tech = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JComboBox<Integer> punct = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JComboBox<Integer> team = new JComboBox<>(new Integer[]{1,2,3,4,5});
        JTextArea comments = new JTextArea(3, 20);
        comm.setSelectedItem(e.getCommunication());
        tech.setSelectedItem(e.getTechnical());
        punct.setSelectedItem(e.getPunctuality());
        team.setSelectedItem(e.getTeamwork());
        comments.setText(e.getComments() != null ? e.getComments() : "");
        int y=0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Communication:"), gbc); gbc.gridx=1; d.add(comm, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Technical:"), gbc); gbc.gridx=1; d.add(tech, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Punctuality:"), gbc); gbc.gridx=1; d.add(punct, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Teamwork:"), gbc); gbc.gridx=1; d.add(team, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Comments:"), gbc); gbc.gridx=1; d.add(new JScrollPane(comments), gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Update");
        save.addActionListener(ev -> {
            try {
                e.setCommunication((int)comm.getSelectedItem());
                e.setTechnical((int)tech.getSelectedItem());
                e.setPunctuality((int)punct.getSelectedItem());
                e.setTeamwork((int)team.getSelectedItem());
                e.setComments(comments.getText().trim());
                e.setOverallScore(e.calculateScore());
                new EvaluationDAO().update(e);
                FileLogger.logInfo("Evaluation", "Updated eval #" + e.getEvalId());
                JOptionPane.showMessageDialog(d, "Updated! Score: " + String.format("%.2f", e.getOverallScore()));
                d.dispose();
                loadEvaluationsAsync(); loadReportAsync();
            } catch (Exception ex) { JOptionPane.showMessageDialog(d, "Failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void editSelectedGoal() {
        int row = goalModel.getRowCount();
        if (row == 0) { JOptionPane.showMessageDialog(this, "No goals to edit"); return; }
        String input = JOptionPane.showInputDialog(this, "Enter Goal ID to edit:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int id = Integer.parseInt(input.trim());
            Goal g = new GoalDAO().getById(id);
            if (g == null) { JOptionPane.showMessageDialog(this, "Not found"); return; }
            showEditGoalDialog(g);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
    }

    private void showEditGoalDialog(Goal g) {
        JDialog d = new JDialog(this, "Edit Goal #" + g.getGoalId(), true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField title = new JTextField(g.getTitle(), 20);
        JTextArea desc = new JTextArea(g.getDescription() != null ? g.getDescription() : "", 3, 20);
        JTextField dateField = new JTextField(g.getTargetDate().toString());
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Pending", "In Progress", "Completed"});
        statusCombo.setSelectedItem(g.getStatus());
        int y=0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Title:"), gbc); gbc.gridx=1; d.add(title, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Description:"), gbc); gbc.gridx=1; d.add(new JScrollPane(desc), gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Target Date:"), gbc); gbc.gridx=1; d.add(dateField, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Status:"), gbc); gbc.gridx=1; d.add(statusCombo, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Update Goal");
        save.addActionListener(ev -> {
            try {
                g.setTitle(title.getText().trim());
                g.setDescription(desc.getText().trim());
                g.setTargetDate(LocalDate.parse(dateField.getText().trim()));
                g.setStatus((String)statusCombo.getSelectedItem());
                new GoalDAO().update(g);
                FileLogger.logInfo("Goal", "Updated goal #" + g.getGoalId());
                JOptionPane.showMessageDialog(d, "Goal updated!");
                d.dispose(); loadGoalsAsync();
            } catch (Exception ex) { JOptionPane.showMessageDialog(d, "Failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void editSelectedFeedback() {
        int row = feedbackModel.getRowCount();
        if (row == 0) { JOptionPane.showMessageDialog(this, "No feedback to edit"); return; }
        String input = JOptionPane.showInputDialog(this, "Enter Feedback ID to edit:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int id = Integer.parseInt(input.trim());
            Feedback f = new FeedbackDAO().getById(id);
            if (f == null) { JOptionPane.showMessageDialog(this, "Not found"); return; }
            showEditFeedbackDialog(f);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
    }

    private void showEditFeedbackDialog(Feedback f) {
        JDialog d = new JDialog(this, "Edit Feedback #" + f.getFeedbackId(), true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<Integer> rating = new JComboBox<>(new Integer[]{1,2,3,4,5});
        rating.setSelectedItem(f.getRating());
        JTextArea comments = new JTextArea(f.getComments() != null ? f.getComments() : "", 3, 20);
        int y=0;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Rating (1-5):"), gbc); gbc.gridx=1; d.add(rating, gbc); y++;
        gbc.gridx=0; gbc.gridy=y; d.add(new JLabel("Comments:"), gbc); gbc.gridx=1; d.add(new JScrollPane(comments), gbc); y++;
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2;
        JButton save = new JButton("Update Feedback");
        save.addActionListener(ev -> {
            try {
                int r = (int) rating.getSelectedItem();
                util.Validator.validateRating(r);
                if (comments.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(d, "Enter comments"); return; }
                f.setRating(r);
                f.setComments(comments.getText().trim());
                new FeedbackDAO().update(f);
                FileLogger.logInfo("Feedback", "Updated feedback #" + f.getFeedbackId());
                JOptionPane.showMessageDialog(d, "Feedback updated!");
                d.dispose(); loadFeedbackAsync();
            } catch (exception.InvalidRatingException ex) { JOptionPane.showMessageDialog(d, ex.getMessage()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(d, "Failed: " + ex.getMessage()); }
        });
        d.add(save, gbc);
        d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void deleteSelectedEvaluation() {
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
