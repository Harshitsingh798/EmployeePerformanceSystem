package ui;

import dao.EvaluationDAO;
import dao.FeedbackDAO;
import dao.GoalDAO;
import exception.GoalDeadlineException;
import model.Evaluation;
import model.Feedback;
import model.Goal;
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The employee's workspace.
 *
 * <p>Five sections - performance tracking, feedback history, personal goals
 * and performance reports - all backed by the live database. Every query is
 * scoped to the signed-in employee, so an employee only ever sees their own
 * rows.
 *
 * <p>The UI uses the same dark-navy left sidebar with a {@link CardLayout}
 * main workspace as the manager dashboard, matching the modern Light SaaS
 * Sidebar Redesign. All database access happens on a {@link SwingWorker}, so
 * the interface never blocks while a query runs.
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

    private final JLabel statusLabel = new JLabel("  Loading your data...");

    /** The employee's average score label, shown on the tracking card. */
    private final JLabel averageLabel = new JLabel("Average Score: loading...", SwingConstants.CENTER);

    /** The card that holds all workspace views. */
    private final JPanel workspace = new JPanel(new java.awt.CardLayout());

    /** Card names. */
    private static final String CARD_TRACKING = "TRACKING";
    private static final String CARD_FEEDBACK = "FEEDBACK";
    private static final String CARD_GOALS = "GOALS";
    private static final String CARD_REPORTS = "REPORTS";

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
        setSize(1200, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        trackingModel = readOnlyModel("Eval ID", "Date", "Comm", "Tech", "Punct", "Team", "Score", "Comments");
        feedbackModel = readOnlyModel("Feedback ID", "Manager ID", "Rating", "Comments", "Date");
        goalsModel = readOnlyModel("Goal ID", "Title", "Type", "Target Date", "Status");

        trackingTable = new JTable(trackingModel);
        feedbackTable = new JTable(feedbackModel);
        goalsTable = new JTable(goalsModel);

        styleTable(trackingTable, new int[]{6}, new int[0]);
        styleTable(feedbackTable, new int[]{2}, new int[0]);
        styleTable(goalsTable, new int[0], new int[]{4});

        // Sidebar navigation.
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put(CARD_TRACKING, "Performance Tracking");
        sections.put(CARD_FEEDBACK, "Feedback History");
        sections.put(CARD_GOALS, "Personal Goals");
        sections.put(CARD_REPORTS, "Performance Reports");

        Sidebar sidebar = new Sidebar(employeeName, "Employee (ID: " + employeeId + ")", sections,
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
        workspace.add(createTrackingPanel(), CARD_TRACKING);
        workspace.add(createFeedbackPanel(), CARD_FEEDBACK);
        workspace.add(createGoalsPanel(), CARD_GOALS);
        workspace.add(createReportsPanel(), CARD_REPORTS);

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

        showCard(CARD_TRACKING);
        loadAllAsync();
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
     * Builds the performance tracking view: summary, trend chart and table.
     */
    private JPanel createTrackingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton refresh = new JButton("Refresh");
        Icons.styleSecondary(refresh, "Refresh");
        refresh.setIcon(Icons.vector(Icons.Symbol.REFRESH, 14, Icons.BODY_TEXT));
        refresh.setIconTextGap(6);
        refresh.addActionListener(event -> loadTrackingAsync());

        panel.add(headerWithActions("Performance Tracking",
                "Your own evaluation scores and trend", refresh), BorderLayout.NORTH);

        // Top section: average card + trend chart side by side.
        JPanel top = new JPanel(new BorderLayout(14, 14));
        top.setOpaque(false);
        top.setBorder(new javax.swing.border.EmptyBorder(0, 0, 14, 0));

        RoundedPanel avgCard = new RoundedPanel(14);
        avgCard.setBackground(Icons.CARD_BG);
        avgCard.setLayout(new BorderLayout(10, 10));
        avgCard.setPreferredSize(new java.awt.Dimension(280, 0));
        JLabel avgTitle = new JLabel("Your Average Score", SwingConstants.LEFT);
        avgTitle.setFont(Icons.bold(14f));
        avgTitle.setForeground(Icons.HEADING_TEXT);
        avgTitle.setBorder(new javax.swing.border.EmptyBorder(2, 2, 6, 2));
        avgCard.add(avgTitle, BorderLayout.NORTH);
        avgCard.add(averageLabel, BorderLayout.CENTER);
        averageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        averageLabel.setFont(Icons.bold(20f));
        averageLabel.setForeground(Icons.PRIMARY);
        top.add(avgCard, BorderLayout.WEST);

        RoundedPanel chartCard = new RoundedPanel(14);
        chartCard.setBackground(Icons.CARD_BG);
        chartCard.setLayout(new BorderLayout());
        JLabel chartTitle = new JLabel("Your Score Over Time", SwingConstants.LEFT);
        chartTitle.setFont(Icons.bold(14f));
        chartTitle.setForeground(Icons.HEADING_TEXT);
        chartTitle.setBorder(new javax.swing.border.EmptyBorder(2, 2, 8, 2));
        chartCard.add(chartTitle, BorderLayout.NORTH);
        trackingChartSlot.setBackground(Color.WHITE);
        chartCard.add(trackingChartSlot, BorderLayout.CENTER);
        top.add(chartCard, BorderLayout.CENTER);

        panel.add(top, BorderLayout.CENTER);

        // Table card at the bottom with bounded scroll height.
        RoundedPanel tableCard = new RoundedPanel(14);
        tableCard.setBackground(Icons.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.setPreferredSize(new java.awt.Dimension(0, 200));
        tableCard.add(cleanScroll(trackingTable), BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Builds the feedback history view.
     */
    private JPanel createFeedbackPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton refresh = new JButton("Refresh");
        Icons.styleSecondary(refresh, "Refresh");
        refresh.setIcon(Icons.vector(Icons.Symbol.REFRESH, 14, Icons.BODY_TEXT));
        refresh.setIconTextGap(6);
        refresh.addActionListener(event -> loadFeedbackAsync());

        panel.add(headerWithActions("Feedback History",
                "Feedback received from your managers", refresh), BorderLayout.NORTH);

        RoundedPanel tableCard = new RoundedPanel(14);
        tableCard.setBackground(Icons.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(cleanScroll(feedbackTable), BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds the personal goals view: table plus a status donut chart.
     */
    private JPanel createGoalsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton add = new JButton("Set Personal Goal");
        Icons.stylePrimary(add, "Set Personal Goal");
        add.setIcon(Icons.vector(Icons.Symbol.PLUS, 14, Color.WHITE));
        add.setIconTextGap(6);
        add.addActionListener(event -> showAddPersonalGoalDialog());

        JButton complete = new JButton("Mark Complete");
        Icons.styleSecondary(complete, "Mark Complete");
        complete.setIcon(Icons.vector(Icons.Symbol.CHECK, 14, Icons.STATUS_COMPLETED));
        complete.setIconTextGap(6);
        complete.addActionListener(event -> markGoalComplete());

        JButton edit = new JButton("Edit Selected");
        Icons.styleSecondary(edit, "Edit Selected");
        edit.setIcon(Icons.vector(Icons.Symbol.EDIT, 14, Icons.BODY_TEXT));
        edit.setIconTextGap(6);
        edit.addActionListener(event -> editSelectedGoal());

        JButton delete = new JButton("Delete Selected");
        Icons.styleDestructive(delete, "Delete Selected");
        delete.setIcon(Icons.vector(Icons.Symbol.TRASH, 14, Icons.DESTRUCTIVE));
        delete.setIconTextGap(6);
        delete.addActionListener(event -> deleteSelectedGoal());

        JButton refresh = new JButton("Refresh");
        Icons.styleSecondary(refresh, "Refresh");
        refresh.setIcon(Icons.vector(Icons.Symbol.REFRESH, 14, Icons.BODY_TEXT));
        refresh.setIconTextGap(6);
        refresh.addActionListener(event -> loadGoalsAsync());

        panel.add(headerWithActions("Personal Goals",
                "Your goals and their status", add, complete, edit, delete, refresh), BorderLayout.NORTH);

        JPanel body = new JPanel(new java.awt.GridLayout(1, 2, 14, 0));
        body.setOpaque(false);

        RoundedPanel tableCard = new RoundedPanel(14);
        tableCard.setBackground(Icons.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(cleanScroll(goalsTable), BorderLayout.CENTER);
        body.add(tableCard);

        RoundedPanel statusCard = new RoundedPanel(14);
        statusCard.setBackground(Icons.CARD_BG);
        statusCard.setLayout(new BorderLayout());
        JLabel statusTitle = new JLabel("Goal Status Distribution", SwingConstants.LEFT);
        statusTitle.setFont(Icons.bold(14f));
        statusTitle.setForeground(Icons.HEADING_TEXT);
        statusTitle.setBorder(new javax.swing.border.EmptyBorder(2, 2, 8, 2));
        statusCard.add(statusTitle, BorderLayout.NORTH);
        goalStatusSlot.setBackground(Color.WHITE);
        statusCard.add(goalStatusSlot, BorderLayout.CENTER);
        body.add(statusCard);

        panel.add(body, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds the performance reports view with the bar and donut charts.
     */
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton export = new JButton("Export My Evaluations to CSV");
        Icons.styleSecondary(export, "Export My Evaluations to CSV");
        export.setIcon(Icons.vector(Icons.Symbol.DOWNLOAD, 14, Icons.BODY_TEXT));
        export.setIconTextGap(6);
        export.addActionListener(event -> exportMyReport());

        panel.add(headerWithActions("Performance Reports",
                "Your criteria averages and export", export), BorderLayout.NORTH);

        JPanel charts = new JPanel(new java.awt.GridLayout(1, 2, 14, 14));
        charts.setOpaque(false);

        RoundedPanel barCard = new RoundedPanel(14);
        barCard.setBackground(Icons.CARD_BG);
        barCard.setLayout(new BorderLayout());
        JLabel barTitle = new JLabel("Your Criteria Averages (Bar)", SwingConstants.LEFT);
        barTitle.setFont(Icons.bold(14f));
        barTitle.setForeground(Icons.HEADING_TEXT);
        barTitle.setBorder(new javax.swing.border.EmptyBorder(2, 2, 8, 2));
        barCard.add(barTitle, BorderLayout.NORTH);
        reportsBarSlot.setBackground(Color.WHITE);
        barCard.add(reportsBarSlot, BorderLayout.CENTER);
        charts.add(barCard);

        RoundedPanel pieCard = new RoundedPanel(14);
        pieCard.setBackground(Icons.CARD_BG);
        pieCard.setLayout(new BorderLayout());
        JLabel pieTitle = new JLabel("Criteria Breakdown (Donut)", SwingConstants.LEFT);
        pieTitle.setFont(Icons.bold(14f));
        pieTitle.setForeground(Icons.HEADING_TEXT);
        pieTitle.setBorder(new javax.swing.border.EmptyBorder(2, 2, 8, 2));
        pieCard.add(pieTitle, BorderLayout.NORTH);
        reportsPieSlot.setBackground(Color.WHITE);
        pieCard.add(reportsPieSlot, BorderLayout.CENTER);
        charts.add(pieCard);

        panel.add(charts, BorderLayout.CENTER);
        return panel;
    }

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
    private void styleTable(JTable table, int[] scoreCols, int[] statusCols) {
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
    private void loadAllAsync() {
        loadTrackingAsync();
        loadFeedbackAsync();
        loadGoalsAsync();
        loadReportAsync();
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
                                "<html><div style='text-align:center;'>"
                                        + "<span style='font-size:24px;color:#4F46E5;'><b>%.2f</b></span>"
                                        + "<span style='font-size:13px;color:#64748B;'> / 5.0</span><br>"
                                        + "<span style='font-size:11px;color:#64748B;'>%d evaluation%s</span>"
                                        + "</div></html>",
                                average, evaluations.size(), evaluations.size() == 1 ? "" : "s"));
                        averageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        List<LocalDate> dates = new ArrayList<>();
                        List<Double> scores = new ArrayList<>();
                        for (Evaluation evaluation : evaluations) {
                            dates.add(evaluation.getEvalDate());
                            scores.add(evaluation.calculateScore());
                        }
                        trackingChartSlot.add(ChartPanel.line(null, dates, scores),
                                BorderLayout.CENTER);
                    }
                    trackingChartSlot.revalidate();
                    trackingChartSlot.repaint();
                    statusLabel.setText("  Tracking loaded: " + evaluations.size() + " evaluations");
                    FileLogger.logInfo("Tracking", "Employee " + employeeId + " loaded " + evaluations.size()
                            + " evaluations");
                } catch (ExecutionException e) {
                    Exception error = unwrap(e);
                    averageLabel.setText("Load failed: " + error.getMessage());
                    statusLabel.setText("  Tracking load failed: " + error.getMessage());
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
     * Loads this employee's goals and refreshes the status donut chart.
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
                    goalStatusSlot.add(ChartPanel.donut(null, pieData, false),
                            BorderLayout.CENTER);
                    goalStatusSlot.revalidate();
                    goalStatusSlot.repaint();
                    loadReportAsync();
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
                    reportsPieSlot.add(ChartPanel.donut(null, criteria, true),
                            BorderLayout.CENTER);
                    reportsBarSlot.removeAll();
                    reportsBarSlot.add(ChartPanel.bar(null, "Rating", criteria),
                            BorderLayout.CENTER);
                    reportsPieSlot.revalidate();
                    reportsPieSlot.repaint();
                    reportsBarSlot.revalidate();
                    reportsBarSlot.repaint();
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

    // ------------------------------------------------------------------ dialogs

    /**
     * Opens the dialog that creates a personal goal.
     */
    private void showAddPersonalGoalDialog() {
        JDialog dialog = new JDialog(this, "Set Personal Goal", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
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
        Icons.stylePrimary(save, "Save Personal Goal");
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
                util.ReportExporter.exportEvaluationsToCSV(evaluations, path);
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
