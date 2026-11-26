package ui.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CenterTextMode;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wraps JFreeChart so that the dashboards do not have to know about the
 * charting library.
 *
 * <p>All charts use the clean Light SaaS look: pure white backgrounds, no grey
 * boxes or plot outlines, subtle dashed gridlines, flat indigo bars with value
 * labels, and modern {@link RingPlot} donut charts with centered summary text.
 */
public final class ChartPanel {

    /** SaaS palette. */
    private static final Color INDIGO = new Color(0x4F46E5);
    private static final Color TEAL = new Color(0x14B8A6);
    private static final Color AMBER = new Color(0xF59E0B);
    private static final Color EMERALD = new Color(0x10B981);
    private static final Color ROSE = new Color(0xEF4444);
    private static final Color GRIDLINE = new Color(0xE2E8F0);
    private static final Color AXIS_LINE = new Color(0xCBD5E1);
    private static final Color FONT_COLOR = new Color(0x0F172A);
    private static final Color BODY_COLOR = new Color(0x334155);
    private static final Color MUTED = new Color(0x64748B);

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font AXIS_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font LEGEND_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    private ChartPanel() {
        // Utility class - not instantiable.
    }

    /**
     * Creates a modern flat bar chart.
     *
     * @param title  optional chart title (pass {@code null} when card already has a header)
     * @param yLabel the label on the value axis
     * @param data   the values, keyed by category name
     * @return a panel containing the chart
     */
    public static JPanel bar(String title, String yLabel, Map<String, Double> data) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        if (data == null || data.isEmpty()) {
            empty(panel, "No evaluation data yet");
            return panel;
        }
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.addValue(entry.getValue(), "Score", entry.getKey());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                title, null, yLabel, dataset, PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart, data.size());
        panel.add(wrapChartPanel(chart), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates a modern donut chart ({@link RingPlot}) with centered average text.
     *
     * @param title optional chart title
     * @param data  the values, keyed by slice name
     * @return a panel containing the chart
     */
    public static JPanel donut(String title, Map<String, Double> data) {
        return donut(title, data, true);
    }

    /**
     * Creates a modern donut chart ({@link RingPlot}).
     *
     * @param title       optional chart title
     * @param data        the values, keyed by slice name
     * @param showAverage {@code true} to show average score in center; {@code false} to show total count
     * @return a panel containing the chart
     */
    public static JPanel donut(String title, Map<String, Double> data, boolean showAverage) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        if (data == null || data.isEmpty()) {
            empty(panel, "No criteria data yet");
            return panel;
        }
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        double total = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
            total += entry.getValue();
        }
        JFreeChart chart = ChartFactory.createRingChart(title, dataset, true, true, false);
        String centerText = showAverage
                ? String.format(Locale.ROOT, "%.2f", total / data.size())
                : String.format(Locale.ROOT, "%.0f", total);
        styleDonutChart(chart, centerText);
        panel.add(wrapChartPanel(chart), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates a line chart of a score over time.
     */
    public static JPanel line(String title, List<LocalDate> dates, List<Double> scores) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        if (dates == null || dates.isEmpty() || scores == null || scores.size() != dates.size()) {
            empty(panel, "No evaluations to plot");
            return panel;
        }
        XYSeries series = new XYSeries("Overall Score", false, true);
        ZoneId zone = ZoneId.systemDefault();
        double first = Double.MAX_VALUE;
        double last = -Double.MAX_VALUE;
        int points = 0;
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            if (date != null) {
                double millis = date.atStartOfDay(zone).toInstant().toEpochMilli();
                series.add(millis, scores.get(i));
                first = Math.min(first, millis);
                last = Math.max(last, millis);
                points++;
            }
        }
        if (points == 0) {
            empty(panel, "No evaluations to plot");
            return panel;
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                title, null, "Score / 5.0", dataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, INDIGO);
        renderer.setSeriesOutlinePaint(0, Color.WHITE);
        renderer.setSeriesFillPaint(0, INDIGO);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.8f));
        renderer.setUseFillPaint(true);
        renderer.setDefaultShapesVisible(true);
        plot.setRenderer(renderer);

        FixedDateAxis dateAxis = new FixedDateAxis("Date");
        dateAxis.setLabelFont(AXIS_FONT);
        dateAxis.setTickLabelFont(AXIS_FONT);
        dateAxis.setTickLabelPaint(MUTED);
        dateAxis.setAxisLinePaint(AXIS_LINE);
        plot.setDomainAxis(dateAxis);
        long oneDay = 86_400_000L;
        long pad = points == 1 ? oneDay : Math.max(oneDay, (long) ((last - first) * 0.05));
        dateAxis.setAutoRange(false);
        dateAxis.setRange(new org.jfree.data.Range(first - pad, last + pad));

        DateFormat format = new SimpleDateFormat("dd MMM", Locale.ROOT);
        long spanDays = Math.max(1L, Math.round((last - first) / (double) oneDay) + 2L);
        long tickEvery = Math.max(1L, spanDays / 6L);
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, (int) tickEvery, format));
        dateAxis.setDateFormatOverride(format);

        NumberAxis valueAxis = (NumberAxis) plot.getRangeAxis();
        valueAxis.setRange(0.0, 5.2);
        valueAxis.setLabelFont(AXIS_FONT);
        valueAxis.setTickLabelFont(AXIS_FONT);
        valueAxis.setTickLabelPaint(MUTED);
        valueAxis.setAxisLineVisible(false);

        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(GRIDLINE);
        plot.setRangeGridlineStroke(new BasicStroke(0.9f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));

        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(TITLE_FONT);
            chart.getTitle().setPaint(FONT_COLOR);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setFrame(BlockBorder.NONE);
            chart.getLegend().setPosition(RectangleEdge.BOTTOM);
            chart.getLegend().setItemFont(LEGEND_FONT);
            chart.getLegend().setItemPaint(BODY_COLOR);
        }

        panel.add(wrapChartPanel(chart), BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------ styling

    private static org.jfree.chart.ChartPanel wrapChartPanel(JFreeChart chart) {
        org.jfree.chart.ChartPanel cp = new org.jfree.chart.ChartPanel(chart);
        cp.setBackground(Color.WHITE);
        cp.setBorder(null);
        cp.setPopupMenu(null);
        cp.setMouseWheelEnabled(false);
        return cp;
    }

    private static void styleBarChart(JFreeChart chart, int categoryCount) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(TITLE_FONT);
            chart.getTitle().setPaint(FONT_COLOR);
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(GRIDLINE);
        plot.setRangeGridlineStroke(new BasicStroke(0.9f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLabelFont(AXIS_FONT);
        rangeAxis.setLabelPaint(MUTED);
        rangeAxis.setTickLabelFont(AXIS_FONT);
        rangeAxis.setTickLabelPaint(MUTED);
        rangeAxis.setAxisLineVisible(false);
        rangeAxis.setTickMarksVisible(false);
        rangeAxis.setRange(0.0, 5.4);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLabelFont(AXIS_FONT);
        domainAxis.setTickLabelFont(AXIS_FONT);
        domainAxis.setTickLabelPaint(BODY_COLOR);
        domainAxis.setAxisLinePaint(AXIS_LINE);
        domainAxis.setTickMarksVisible(false);
        domainAxis.setCategoryLabelPositions(categoryCount <= 6
                ? CategoryLabelPositions.STANDARD
                : CategoryLabelPositions.UP_45);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, INDIGO);
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setMaximumBarWidth(0.14);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelGenerator(
                new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat("0.00")));
        renderer.setDefaultItemLabelFont(VALUE_FONT);
        renderer.setDefaultItemLabelPaint(FONT_COLOR);

        if (chart.getLegend() != null) {
            chart.getLegend().setFrame(BlockBorder.NONE);
            chart.getLegend().setItemFont(LEGEND_FONT);
        }
    }

    private static void styleDonutChart(JFreeChart chart, String centerText) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(TITLE_FONT);
            chart.getTitle().setPaint(FONT_COLOR);
        }

        RingPlot plot = (RingPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setSectionDepth(0.30);
        plot.setSeparatorsVisible(true);
        plot.setSeparatorPaint(Color.WHITE);
        plot.setSeparatorStroke(new BasicStroke(2.5f));
        plot.setLabelGenerator(null); // clean legend instead of callout boxes
        plot.setSimpleLabels(false);
        plot.setStartAngle(90);
        plot.setInteriorGap(0.04);

        plot.setCenterTextMode(CenterTextMode.FIXED);
        plot.setCenterText(centerText);
        plot.setCenterTextFont(new Font("Segoe UI", Font.BOLD, 28));
        plot.setCenterTextColor(FONT_COLOR);

        Color[] colours = {INDIGO, TEAL, AMBER, EMERALD, ROSE};
        List<?> keys = plot.getDataset().getKeys();
        for (int i = 0; i < keys.size(); i++) {
            Comparable<?> key = (Comparable<?>) keys.get(i);
            String label = key.toString().toLowerCase(Locale.ROOT);
            Color sectionColor = colours[i % colours.length];
            if (label.contains("completed")) {
                sectionColor = EMERALD;
            } else if (label.contains("progress")) {
                sectionColor = INDIGO;
            } else if (label.contains("pending")) {
                sectionColor = AMBER;
            }
            plot.setSectionPaint(key, sectionColor);
            plot.setSectionOutlinePaint(key, Color.WHITE);
            plot.setSectionOutlineStroke(key, new BasicStroke(2.0f));
        }

        if (chart.getLegend() != null) {
            chart.getLegend().setFrame(BlockBorder.NONE);
            chart.getLegend().setPosition(RectangleEdge.BOTTOM);
            chart.getLegend().setItemFont(LEGEND_FONT);
            chart.getLegend().setItemPaint(BODY_COLOR);
            chart.getLegend().setLegendItemGraphicPadding(new RectangleInsets(2, 6, 2, 4));
            plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator("{0} ({2})"));
        }
    }

    private static void empty(JPanel panel, String message) {
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        label.setForeground(MUTED);
        panel.add(label, BorderLayout.CENTER);
    }

    private static final class FixedDateAxis extends DateAxis {

        private static final long serialVersionUID = 1L;

        FixedDateAxis(String label) {
            super(label);
        }

        @Override
        protected void selectAutoTickUnit(java.awt.Graphics2D g2,
                                          java.awt.geom.Rectangle2D dataArea,
                                          RectangleEdge edge) {
            // Keep the tick unit that was set explicitly.
        }

        @Override
        protected void selectHorizontalAutoTickUnit(java.awt.Graphics2D g2,
                                                    java.awt.geom.Rectangle2D dataArea,
                                                    RectangleEdge edge) {
            // Keep the tick unit that was set explicitly.
        }

        @Override
        protected void selectVerticalAutoTickUnit(java.awt.Graphics2D g2,
                                                  java.awt.geom.Rectangle2D dataArea,
                                                  RectangleEdge edge) {
            // Keep the tick unit that was set explicitly.
        }
    }
}
