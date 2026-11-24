package ui.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.DateFormat;
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
 * <p>Every method returns a ready-to-add Swing panel; an empty data set
 * produces a friendly placeholder instead of a blank chart area.
 */
public final class ChartPanel {

    private ChartPanel() {
        // Utility class - not instantiable.
    }

    /**
     * Creates a bar chart.
     *
     * @param title  the chart title
     * @param yLabel the label on the value axis
     * @param data   the values, keyed by category name
     * @return a panel containing the chart
     */
    public static JPanel bar(String title, String yLabel, Map<String, Double> data) {
        JPanel panel = wrap();
        if (data == null || data.isEmpty()) {
            empty(panel, "No evaluation data yet");
            return panel;
        }
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.addValue(entry.getValue(), "Score", entry.getKey());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                title, "Employee", yLabel, dataset, PlotOrientation.VERTICAL, false, true, false);
        panel.add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates a pie chart.
     *
     * @param title the chart title
     * @param data  the values, keyed by slice name
     * @return a panel containing the chart
     */
    public static JPanel pie(String title, Map<String, Double> data) {
        JPanel panel = wrap();
        if (data == null || data.isEmpty()) {
            empty(panel, "No criteria data yet");
            return panel;
        }
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        panel.add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates a line chart of a score over time.
     *
     * <p>The x-axis is a real date axis, and two evaluations recorded on the
     * same day are plotted as two separate points rather than overwriting each
     * other.
     *
     * @param title  the chart title
     * @param dates  the evaluation dates
     * @param scores the matching scores
     * @return a panel containing the chart
     */
    public static JPanel line(String title, List<LocalDate> dates, List<Double> scores) {
        JPanel panel = wrap();
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
                // Milliseconds since the epoch - the value a DateAxis expects.
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
                title, "Date", "Score / 5.0", dataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();

        // Draw a marker at every point, so a single evaluation is still visible.
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, new Color(0, 105, 92));
        renderer.setSeriesOutlinePaint(0, new Color(0, 105, 92));
        renderer.setSeriesFillPaint(0, new Color(0, 105, 92));
        plot.setRenderer(renderer);

        // Fix the date range by hand. Left on auto, a single point collapses
        // the axis to a few hours and the labels turn into times of day
        // instead of dates. The range has to be set *after* the axis is
        // installed, otherwise installing it resets the range again.
        FixedDateAxis dateAxis = new FixedDateAxis("Date");
        plot.setDomainAxis(dateAxis);
        long oneDay = 86_400_000L;
        long pad = points == 1 ? oneDay : Math.max(oneDay, (long) ((last - first) * 0.05));
        dateAxis.setAutoRange(false);
        dateAxis.setRange(new org.jfree.data.Range(first - pad, last + pad));

        // A fixed range leaves the tick unit from the old auto-ranging in
        // place, which is why the labels would otherwise read "00:00" and
        // "18:00" instead of dates. Pick a day-based unit to match.
        DateFormat format = new SimpleDateFormat("dd MMM", Locale.ROOT);
        long spanDays = Math.max(1L, Math.round((last - first) / (double) oneDay) + 2L);
        long tickEvery = Math.max(1L, spanDays / 6L);
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, (int) tickEvery, format));
        dateAxis.setDateFormatOverride(format);

        // The score is out of five, so pin the value axis to the same range
        // the ratings actually use.
        org.jfree.chart.axis.NumberAxis valueAxis =
                (org.jfree.chart.axis.NumberAxis) plot.getRangeAxis();
        valueAxis.setRange(0.0, 5.0);

        panel.add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        return panel;
    }

    /**
     * A date axis that keeps the tick unit it was given.
     *
     * <p>{@link DateAxis} normally re-picks its tick unit every time the
     * chart is drawn, which for a short date range means hourly ticks and
     * labels such as "00:00" and "18:00" in place of dates. This subclass
     * keeps the day-based unit the caller asked for.
     */
    private static final class FixedDateAxis extends DateAxis {

        private static final long serialVersionUID = 1L;

        FixedDateAxis(String label) {
            super(label);
        }

        @Override
        protected void selectAutoTickUnit(java.awt.Graphics2D g2,
                                          java.awt.geom.Rectangle2D dataArea,
                                          org.jfree.chart.ui.RectangleEdge edge) {
            // Keep the tick unit that was set explicitly.
        }

        @Override
        protected void selectHorizontalAutoTickUnit(java.awt.Graphics2D g2,
                                                    java.awt.geom.Rectangle2D dataArea,
                                                    org.jfree.chart.ui.RectangleEdge edge) {
            // Keep the tick unit that was set explicitly.
        }

        @Override
        protected void selectVerticalAutoTickUnit(java.awt.Graphics2D g2,
                                                  java.awt.geom.Rectangle2D dataArea,
                                                  org.jfree.chart.ui.RectangleEdge edge) {
            // Keep the tick unit that was set explicitly.
        }
    }

    /**
     * Creates the empty container that a chart is added to.
     */
    private static JPanel wrap() {
        return new JPanel(new BorderLayout());
    }

    /**
     * Shows a placeholder where a chart would be.
     */
    private static void empty(JPanel panel, String message) {
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.CENTER);
    }
}
