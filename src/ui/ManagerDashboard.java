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
import service.ReportService;
import ui.components.ChartPanel;
import ui.components.KPICard;
import ui.components.RoundedPanel;
import ui.components.Sidebar;
import ui.components.TableRenderers;
import util.FileLogger;
import util.Icons;
import util.Validator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The manager's workspace.
 *
 * <p>Five sections - evaluation, goals, feedback, reports and team management -
 * all backed by the live database. Every query is scoped to the signed-in
 * manager, so a manager only ever sees their own team.
 *
 * <p>The UI uses a dark-navy left sidebar with a {@link CardLayout} main
 * workspace, matching the modern Light SaaS Sidebar Redesign. All database
 * access happens on a {@link SwingWorker}, so the interface never blocks while
 * a query runs.
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

    /** The card that holds all workspace views. */
    private final JPanel workspace = new JPanel(new java.awt.CardLayout());

    /** The search field, declared here so the debounce timer can read it. */
    private final JTextField searchField = new JTextField(20);

    /** The search worker currently in flight, so a new keystroke can cancel it. */
    private transient SwingWorker<List<Evaluation>, Void> searchWorker;

    /** Debounce timer that delays the search until typing stops. */
    private final Timer searchTimer;

    /** The employees reporting to this manager - used to populate the pickers. */
    private transient List<Employee> teamMembers = new ArrayList<>();

    /** Card names. */
    private static final String CARD_EVAL = "EVALUATIONS";
    private static final String CARD_GOALS = "GOALS";
    private static final String CARD_FEEDBACK = "FEEDBACK";
    private static final String CARD_REPORTS = "REPORTS";
    private static final String CARD_TEAM = "TEAM";

    /** Slots for the report charts, populated by {@link #loadReportAsync}. */
    private final JPanel reportBarSlot = new JPanel(new BorderLayout());
    private final JPanel reportPieSlot = new JPanel(new BorderLayout());

    /** KPI strip for the evaluation view, populated after data loads. */
    private JPanel evalKpiStrip;
    private KPICard teamAvgKpi;
    private KPICard totalEvalKpi;
    private KPICard topPerformerKpi;
    private KPICard pendingGoalsKpi;

    /** Report insight cards, updated when report averages load. */
    private KPICard strongestSkillCard;
    private KPICard improveSkillCard;

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
        setSize(1200, 720);
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

        styleTable(evalTable, 1, new int[]{6}, new int[0]);
        styleTable(goalTable, -1, new int[0], new int[]{4});
        styleTable(feedbackTable, -1, new int[]{2}, new int[0]);
        styleTable(teamTable, 1, new int[0], new int[0]);

        searchTimer = new Timer(SEARCH_DEBOUNCE_MS, event -> runSearch(searchField.getText()));
        searchTimer.setRepeats(false);

        // Sidebar navigation. Each nav button is named with its card key so the
        // listener knows which workspace card to show.
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put(CARD_EVAL, "Performance Evaluation");
        sections.put(CARD_GOALS, "Goal Setting");
        sections.put(CARD_FEEDBACK, "Feedback Provision");
        sections.put(CARD_REPORTS, "Performance Reports");
        sections.put(CARD_TEAM, "Team Management");

        Sidebar sidebar = new Sidebar(managerName, "Manager (ID: " + managerId + ")", sections,
                event -> {
                    javax.swing.AbstractButton button = (javax.swing.AbstractButton) event.getSource();
                    showCard(button.getName());
                });
        sidebar.addLogout(event -> logout());
        int index = 0;
        for (String key : sections.keySet()) {
            sidebar.navButtonAt(index).setName(key);
            index++;
        }

        // Build the workspace cards.
        workspace.setOpaque(false);
        workspace.setBorder(new javax.swing.border.EmptyBorder(18, 24, 16, 24));
        workspace.add(createEvaluationPanel(), CARD_EVAL);
        workspace.add(createGoalPanel(), CARD_GOALS);
        workspace.add(createFeedbackPanel(), CARD_FEEDBACK);
        workspace.add(createReportsPanel(), CARD_REPORTS);
        workspace.add(createTeamPanel(), CARD_TEAM);

        // Main layout: sidebar + workspace.
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Icons.CANVAS_BG);
        root.add(sidebar, BorderLayout.WEST);
        root.add(workspace, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);

        statusLabel.setFont(Icons.font(11f));
        statusLabel.setForeground(Icons.MUTED_TEXT);
        statusLabel.setBackground(Icons.CANVAS_BG);
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        add(statusLabel, BorderLayout.SOUTH);

        showCard(CARD_EVAL);
        loadAllDataAsync();
    }

    /**
     * Shows the workspace card with the given name.
     *
     * @param card the card name
     */
    private void showCard(String card) {
        java.awt.CardLayout layout = (java.awt.CardLayout) workspace.getLayout();
        layout.show(workspace, card);
    }

    /**
     * Signs out and returns to the login screen.
     */
    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    // ------------------------------------------------------------------ panels

    /**
     * Builds the evaluation view: a searchable table plus the CRUD buttons.
     */
    private JPanel createEvaluationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Page header + toolbar + KPI strip all live in the north region as a
        // vertical stack.
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);

        north.add(pageHeader("Performance Evaluation",
                "Track your team's performance evaluations"));

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new javax.swing.border.EmptyBorder(8, 0, 10, 0));
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField.setToolTipText("Search by employee name or comments");
        searchField.putClientProperty("JTextField.placeholderText", "Search employees, comments...");
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

        JPanel searchWrap = new JPanel(new BorderLayout(8, 0));
        searchWrap.setOpaque(false);
        Icons.styleInput(searchField);
        searchField.setPreferredSize(new java.awt.Dimension(260, 36));
        JLabel searchIcon = new JLabel(Icons.vector(Icons.Symbol.SEARCH, 15, Icons.MUTED_TEXT));
        searchWrap.add(searchIcon, BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);

        JButton add = new JButton("Add Evaluation");
        Icons.stylePrimary(add, "Add Evaluation");
        add.setIcon(Icons.vector(Icons.Symbol.PLUS, 14, Color.WHITE));
        add.setIconTextGap(6);
        add.addActionListener(event -> showAddEvaluationDialog());

        JButton edit = new JButton("Edit Selected");
        Icons.styleSecondary(edit, "Edit Selected");
        edit.setIcon(Icons.vector(Icons.Symbol.EDIT, 14, Icons.BODY_TEXT));
        edit.setIconTextGap(6);
        edit.addActionListener(event -> editSelectedEvaluation());

        JButton refresh = new JButton("Refresh");
        Icons.styleSecondary(refresh, "Refresh");
        refresh.setIcon(Icons.vector(Icons.Symbol.REFRESH, 14, Icons.BODY_TEXT));
        refresh.setIconTextGap(6);
        refresh.addActionListener(event -> loadEvaluationsAsync());

        JButton delete = new JButton("Delete Selected");
        Icons.styleDestructive(delete, "Delete Selected");
        delete.setIcon(Icons.vector(Icons.Symbol.TRASH, 14, Icons.DESTRUCTIVE));
        delete.setIconTextGap(6);
        delete.addActionListener(event -> deleteSelectedEvaluation());

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(edit);
        actions.add(refresh);
        actions.add(delete);

        toolbar.add(searchWrap, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);
        north.add(toolbar);

        // KPI strip.
        evalKpiStrip = new JPanel(new java.awt.GridLayout(1, 4, 14, 0));
        evalKpiStrip.setOpaque(false);
        evalKpiStrip.setBorder(new javax.swing.border.EmptyBorder(4, 0, 14, 0));
        evalKpiStrip.setAlignmentX(Component.LEFT_ALIGNMENT);
        teamAvgKpi = new KPICard("Team Average", "--", "Average across evaluations", Icons.PRIMARY, Icons.Symbol.STAR);
        totalEvalKpi = new KPICard("Total Evaluations", "--", "Recorded by you", Icons.MUTED_TEXT, Icons.Symbol.CLIPBOARD);
        topPerformerKpi = new KPICard("Top Performer", "--", "Highest score", Icons.STATUS_COMPLETED, Icons.Symbol.TROPHY);
        pendingGoalsKpi = new KPICard("Pending Goals", "--", "In-progress targets", Icons.STATUS_IN_PROGRESS, Icons.Symbol.TARGET);
        evalKpiStrip.add(teamAvgKpi);
        evalKpiStrip.add(totalEvalKpi);
        evalKpiStrip.add(topPerformerKpi);
        evalKpiStrip.add(pendingGoalsKpi);
        north.add(evalKpiStrip);

        panel.add(north, BorderLayout.NORTH);

        // Table card.
        RoundedPanel tableCard = new RoundedPanel(14);
        tableCard.setBackground(Icons.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(cleanScroll(evalTable), BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds the goal view.
     */
    private JPanel createGoalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton add = new JButton("Set New Goal");
        Icons.stylePrimary(add, "Set New Goal");
        add.setIcon(Icons.vector(Icons.Symbol.PLUS, 14, Color.WHITE));
        add.setIconTextGap(6);
        add.addActionListener(event -> showAddGoalDialog());

        JButton edit = new JButton("Edit Selected");
        Icons.styleSecondary(edit, "Edit Selected");
        edit.setIcon(Icons.vector(Icons.Symbol.EDIT, 14, Icons.BODY_TEXT));
        edit.setIconTextGap(6);
        edit.addActionListener(event -> editSelectedGoal());

        JButton refresh = new JButton("Refresh");
        Icons.styleSecondary(refresh, "Refresh");
        refresh.setIcon(Icons.vector(Icons.Symbol.REFRESH, 14, Icons.BODY_TEXT));
        refresh.setIconTextGap(6);
        refresh.addActionListener(event -> loadGoalsAsync());

        panel.add(headerWithActions("Goal Setting",
                "Assign and track goals for your team", add, edit, refresh), BorderLayout.NORTH);

        RoundedPanel tableCard = new RoundedPanel(14);
        tableCard.setBackground(Icons.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(cleanScroll(goalTable), BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds the feedback view.
     */
    private JPanel createFeedbackPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton add = new JButton("Give Feedback");
        Icons.stylePrimary(add, "Give Feedback");
        add.setIcon(Icons.vector(Icons.Symbol.PLUS, 14, Color.WHITE));
        add.setIconTextGap(6);
        add.addActionListener(event -> showAddFeedbackDialog());

        JButton edit = new JButton("Edit Selected");
        Icons.styleSecondary(edit, "Edit Selected");
        edit.setIcon(Icons.vector(Icons.Symbol.EDIT, 14, Icons.BODY_TEXT));
        edit.setIconTextGap(6);
        edit.addActionListener(event -> editSelectedFeedback());

        JButton refresh = new JButton("Refresh");
        Icons.styleSecondary(refresh, "Refresh");
        refresh.setIcon(Icons.vector(Icons.Symbol.REFRESH, 14, Icons.BODY_TEXT));
        refresh.setIconTextGap(6);
        refresh.addActionListener(event -> loadFeedbackAsync());

        panel.add(headerWithActions("Feedback Provision",
                "Give feedback to your team members", add, edit, refresh), BorderLayout.NORTH);

        RoundedPanel tableCard = new RoundedPanel(14);
        tableCard.setBackground(Icons.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(cleanScroll(feedbackTable), BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds the reports view with the bar and donut charts plus insight cards.
     */
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton export = new JButton("Export Team Report (CSV)");
        Icons.styleSecondary(export, "Export Team Report (CSV)");
        export.setIcon(Icons.vector(Icons.Symbol.DOWNLOAD, 14, Icons.BODY_TEXT));
        export.setIconTextGap(6);
        export.addActionListener(event -> exportReport());

        panel.add(headerWithActions("Performance Reports",
                "Team averages, criteria breakdown and export", export), BorderLayout.NORTH);

        // Two chart cards side by side.
        JPanel charts = new JPanel(new java.awt.GridLayout(1, 2, 14, 14));
        charts.setOpaque(false);
        charts.setBorder(new javax.swing.border.EmptyBorder(0, 0, 14, 0));

        RoundedPanel barCard = new RoundedPanel(14);
        barCard.setBackground(Icons.CARD_BG);
        barCard.setLayout(new BorderLayout());
        JLabel barTitle = new JLabel("Avg Score per Employee", SwingConstants.LEFT);
        barTitle.setFont(Icons.bold(14f));
        barTitle.setForeground(Icons.HEADING_TEXT);
        barTitle.setBorder(new javax.swing.border.EmptyBorder(4, 4, 10, 4));
        barCard.add(barTitle, BorderLayout.NORTH);
        reportBarSlot.setBackground(Color.WHITE);
        barCard.add(reportBarSlot, BorderLayout.CENTER);
        charts.add(barCard);

        RoundedPanel donutCard = new RoundedPanel(14);
        donutCard.setBackground(Icons.CARD_BG);
        donutCard.setLayout(new BorderLayout());
        JLabel donutTitle = new JLabel("Criteria Averages", SwingConstants.LEFT);
        donutTitle.setFont(Icons.bold(14f));
        donutTitle.setForeground(Icons.HEADING_TEXT);
        donutTitle.setBorder(new javax.swing.border.EmptyBorder(4, 4, 10, 4));
        donutCard.add(donutTitle, BorderLayout.NORTH);
        reportPieSlot.setBackground(Color.WHITE);
        donutCard.add(reportPieSlot, BorderLayout.CENTER);
        charts.add(donutCard);

        panel.add(charts, BorderLayout.CENTER);

        // Insight strip.
        JPanel insight = new JPanel(new java.awt.GridLayout(1, 2, 14, 0));
        insight.setOpaque(false);
        insight.setBorder(new javax.swing.border.EmptyBorder(4, 0, 0, 0));
        strongestSkillCard = new KPICard("Strongest Skill", "--", "Highest team criterion",
                Icons.STATUS_COMPLETED, Icons.Symbol.TROPHY);
        improveSkillCard = new KPICard("Area to Improve", "--", "Lowest team criterion",
                Icons.STATUS_IN_PROGRESS, Icons.Symbol.TARGET);
        insight.add(strongestSkillCard);
        insight.add(improveSkillCard);
        panel.add(insight, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Builds the team management view.
     */
    private JPanel createTeamPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton addMember = new JButton("Add Member");
        Icons.stylePrimary(addMember, "Add Member");
        addMember.setIcon(Icons.vector(Icons.Symbol.PLUS, 14, Color.WHITE));
        addMember.setIconTextGap(6);
        addMember.addActionListener(event -> showAddMemberDialog(false));

        JButton assignRole = new JButton("Assign Role");
        Icons.styleSecondary(assignRole, "Assign Role");
        assignRole.setIcon(Icons.vector(Icons.Symbol.EDIT, 14, Icons.BODY_TEXT));
        assignRole.setIconTextGap(6);
        assignRole.addActionListener(event -> showAddMemberDialog(true));

        JButton refresh = new JButton("Refresh");
        Icons.styleSecondary(refresh, "Refresh");
        refresh.setIcon(Icons.vector(Icons.Symbol.REFRESH, 14, Icons.BODY_TEXT));
        refresh.setIconTextGap(6);
        refresh.addActionListener(event -> loadTeamAsync());

        panel.add(headerWithActions("Team Management",
                "Manage your team members and their roles", addMember, assignRole, refresh), BorderLayout.NORTH);

        RoundedPanel tableCard = new RoundedPanel(14);
        tableCard.setBackground(Icons.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(cleanScroll(teamTable), BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds a combined header row with title/subtitle on the left and action buttons on the right.
     */
    private JPanel headerWithActions(String title, String subtitle, JButton... buttons) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new javax.swing.border.EmptyBorder(0, 0, 14, 0));
        row.add(pageHeader(title, subtitle), BorderLayout.WEST);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 6));
        actions.setOpaque(false);
        for (JButton button : buttons) {
            actions.add(button);
        }
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JScrollPane cleanScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    // ------------------------------------------------------------------ page header

    /**
     * Builds a consistent page header: title, subtitle and a thin divider.
     *
     * @param title    the page title
     * @param subtitle the page subtitle
     * @return the header panel
     */
    private JPanel pageHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);
        header.setBorder(new javax.swing.border.EmptyBorder(10, 0, 10, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Icons.bold(20f));
        titleLabel.setForeground(Icons.HEADING_TEXT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(Icons.font(13f));
        subtitleLabel.setForeground(Icons.MUTED_TEXT);

        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        return header;
    }

    // ------------------------------------------------------------------ table styling

    /**
     * Applies the modern table style: row height, padding, renderers.
     *
     * @param table    the table to style
     * @param scoreCols column indexes that should render as score pill badges
     */
    private void styleTable(JTable table, int employeeCol, int[] scoreCols, int[] statusCols) {
        table.setRowHeight(46);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xF1F5F9));
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setFont(Icons.font(13f));
        table.setBackground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(TableRenderers.header());
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 38));

        for (int col = 0; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(TableRenderers.cell());
        }

        if (employeeCol >= 0 && employeeCol < table.getColumnCount()) {
            table.getColumnModel().getColumn(employeeCol).setCellRenderer(TableRenderers.employee());
            table.getColumnModel().getColumn(employeeCol).setPreferredWidth(180);
        }

        for (int col : scoreCols) {
            if (col >= 0 && col < table.getColumnCount()) {
                table.getColumnModel().getColumn(col).setCellRenderer(TableRenderers.score());
                table.getColumnModel().getColumn(col).setPreferredWidth(90);
            }
        }

        for (int col : statusCols) {
            if (col >= 0 && col < table.getColumnCount()) {
                table.getColumnModel().getColumn(col).setCellRenderer(TableRenderers.status());
                table.getColumnModel().getColumn(col).setPreferredWidth(120);
            }
        }
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
                    updateEvalKPIs(evaluations);
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
     * Updates the evaluation KPI cards from the loaded data.
     */
    private void updateEvalKPIs(List<Evaluation> evaluations) {
        double teamAverage = 0;
        if (!evaluations.isEmpty()) {
            teamAverage = evaluations.stream()
                    .mapToDouble(Evaluation::calculateScore).average().orElse(0);
        }
        String topPerformer = "--";
        if (!evaluations.isEmpty()) {
            Evaluation top = evaluations.stream()
                    .max(Comparator.comparingDouble(Evaluation::calculateScore))
                    .orElse(null);
            topPerformer = top == null ? "--" : displayName(top.getEmployeeName(), top.getEmployeeId());
        }

        if (teamAvgKpi != null) {
            teamAvgKpi.setValue(evaluations.isEmpty() ? "--" : String.format(Locale.ROOT, "%.2f / 5.0", teamAverage));
        }
        if (totalEvalKpi != null) {
            totalEvalKpi.setValue(String.valueOf(evaluations.size()));
        }
        if (topPerformerKpi != null) {
            topPerformerKpi.setValue(topPerformer);
        }
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
                    int pendingCount = 0;
                    for (Goal goal : goals) {
                        if (!Goal.STATUS_COMPLETED.equalsIgnoreCase(goal.getStatus())) {
                            pendingCount++;
                        }
                        goalModel.addRow(new Object[]{
                                goal.getGoalId(),
                                goal.getTitle(),
                                goal.getAssignedTo(),
                                goal.getTargetDate(),
                                goal.getStatus(),
                                goal.isPersonal() ? "Yes" : "No"});
                    }
                    if (pendingGoalsKpi != null) {
                        pendingGoalsKpi.setValue(String.valueOf(pendingCount));
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

                Map<Integer, String> names = new java.util.HashMap<>();
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
                    reportBarSlot.add(ChartPanel.bar(null, "Score", barData), BorderLayout.CENTER);
                    reportPieSlot.removeAll();
                    reportPieSlot.add(ChartPanel.donut(null, pieData, true), BorderLayout.CENTER);
                    reportBarSlot.revalidate();
                    reportBarSlot.repaint();
                    reportPieSlot.revalidate();
                    reportPieSlot.repaint();

                    if (!pieData.isEmpty()) {
                        Map.Entry<String, Double> strongest = pieData.entrySet().stream()
                                .max(Map.Entry.comparingByValue()).orElse(null);
                        Map.Entry<String, Double> weakest = pieData.entrySet().stream()
                                .min(Map.Entry.comparingByValue()).orElse(null);
                        if (strongest != null && strongestSkillCard != null) {
                            strongestSkillCard.setValue(strongest.getKey() + ": "
                                    + String.format(Locale.ROOT, "%.2f", strongest.getValue()));
                            strongestSkillCard.setSubtitle("Highest team average criterion");
                        }
                        if (weakest != null && improveSkillCard != null) {
                            improveSkillCard.setValue(weakest.getKey() + ": "
                                    + String.format(Locale.ROOT, "%.2f", weakest.getValue()));
                            improveSkillCard.setSubtitle("Focus area for improvement");
                        }
                    }
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

    // ------------------------------------------------------------- search

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
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Save");
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
                    loadTeamAsync();
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
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Save");
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
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Save Goal");
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
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Submit Feedback");
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
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Update");
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
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Update Goal");
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
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Update Feedback");
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
                new ReportService().exportManagerReportCSV(managerId, path);
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
