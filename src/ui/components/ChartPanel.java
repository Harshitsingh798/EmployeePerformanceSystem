package ui.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.*;
import java.time.LocalDate;

public class ChartPanel {

    public static JPanel bar(String title, String yLabel, java.util.Map<String, Double> data) {
        JPanel p = wrap();
        if (data == null || data.isEmpty()) { empty(p, "No evaluation data yet"); return p; }
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (var e : data.entrySet()) ds.addValue(e.getValue(), "Score", e.getKey());
        JFreeChart chart = ChartFactory.createBarChart(title, "Employee", yLabel, ds, PlotOrientation.VERTICAL, false, true, false);
        p.add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        return p;
    }

    public static JPanel pie(String title, java.util.Map<String, Double> data) {
        JPanel p = wrap();
        if (data == null || data.isEmpty()) { empty(p, "No criteria data yet"); return p; }
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        for (var e : data.entrySet()) ds.setValue(e.getKey(), e.getValue());
        JFreeChart chart = ChartFactory.createPieChart(title, ds, true, true, false);
        p.add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        return p;
    }

    public static JPanel line(String title, java.util.List<LocalDate> dates, java.util.List<Double> scores) {
        JPanel p = wrap();
        if (dates == null || dates.isEmpty()) { empty(p, "No evaluations to plot"); return p; }
        TimeSeries series = new TimeSeries("Overall Score");
        for (int i = 0; i < dates.size(); i++) {
            LocalDate d = dates.get(i);
            series.addOrUpdate(new Day(d.getDayOfMonth(), d.getMonthValue(), d.getYear()), scores.get(i));
        }
        TimeSeriesCollection ds = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date", "Score / 5.0", ds, true, true, false);
        p.add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        return p;
    }

    private static JPanel wrap() {
        JPanel p = new JPanel(new BorderLayout());
        return p;
    }

    private static void empty(JPanel p, String msg) {
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lbl.setForeground(Color.GRAY);
        p.add(lbl, BorderLayout.CENTER);
    }
}
