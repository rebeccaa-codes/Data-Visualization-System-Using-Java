package ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChartHelper {
// ================= GENERIC CSV IMPORT =================
    public String[] readHeaders(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String headerLine = reader.readLine();
        reader.close();
        if (headerLine == null) return new String[0];
        return headerLine.split(",");
    }

    public List<String[]> readRows(String filePath) throws IOException {
        List<String[]> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            if (firstLine) {              // skip header
                firstLine = false;
                continue;
            }
            rows.add(line.split(",", -1));  // -1 keeping empty trailing values
        }
        reader.close();
        return rows;
    }
// Checks if a column (by index) contains numbers - used to tell user which columns CAN be charted as values
    public boolean isColumnNumeric(List<String[]> rows, int columnIndex) {
        for (String[] row : rows) {
            if (columnIndex >= row.length) continue;
            String val = row[columnIndex].trim();
            if (val.isEmpty()) continue;
            try {
                Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return false;   // found something that's not a number
            }
        }
        return true;
    }
// Extracts one column as labels (text) - e.g. names, dates, categories
    public List<String> extractLabelColumn(List<String[]> rows, int columnIndex) {
        List<String> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(columnIndex < row.length ? row[columnIndex].trim() : "");
        }
        return result;
    }
 // Extracts one column as numbers - skips/treats bad values as 0.0
    public List<Double> extractNumericColumn(List<String[]> rows, int columnIndex) {
        List<Double> result = new ArrayList<>();
        for (String[] row : rows) {
            double val = 0.0;
            if (columnIndex < row.length) {
                try {
                    val = Double.parseDouble(row[columnIndex].trim());
                } catch (NumberFormatException e) {
                    val = 0.0; // non-numeric cell -> treated as 0
                }
            }
            result.add(val);
        }
        return result;
    }

    // === DATASET BUILDERS ==

    public DefaultCategoryDataset createCategoryDataset(List<String> categories, List<Double> values) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < categories.size(); i++) {
            dataset.addValue(values.get(i), "Data", categories.get(i));
        }
        return dataset;
    }

    public DefaultPieDataset createPieDataset(List<String> categories, List<Double> values) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (int i = 0; i < categories.size(); i++) {
            dataset.setValue(categories.get(i), values.get(i));
        }
        return dataset;
    }

    public XYSeriesCollection createXYDataset(List<Double> xValues, List<Double> yValues, String seriesName) {
        XYSeries series = new XYSeries(seriesName);
        for (int i = 0; i < xValues.size(); i++) {
            series.add(xValues.get(i), yValues.get(i));
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        return dataset;
    }

    // ================= CHART CREATORS =================

    public JFreeChart createBarChart(DefaultCategoryDataset dataset, String title, String xLabel, String yLabel) {
        JFreeChart chart = ChartFactory.createBarChart(title, xLabel, yLabel, dataset);
        styleBarChart(chart);
        return chart;
    }

    public JFreeChart createLineChart(DefaultCategoryDataset dataset, String title, String xLabel, String yLabel) {
        return ChartFactory.createLineChart(title, xLabel, yLabel, dataset);
    }

    public JFreeChart createPieChart(DefaultPieDataset dataset, String title) {
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        stylePieChart(chart);
        return chart;
    }

    public JFreeChart createScatterChart(XYSeriesCollection dataset, String title, String xLabel, String yLabel) {
        return ChartFactory.createScatterPlot(title, xLabel, yLabel, dataset);
    }

    public JFreeChart getChart(String chartType, List<String> categories, List<Double> values) {
        switch (chartType.toUpperCase()) {
            case "BAR":
                return createBarChart(createCategoryDataset(categories, values), "Bar Chart", "Category", "Value");
            case "LINE":
                return createLineChart(createCategoryDataset(categories, values), "Line Chart", "Category", "Value");
            case "PIE":
                return createPieChart(createPieDataset(categories, values), "Pie Chart");
            default:
                throw new IllegalArgumentException("Unknown chart type: " + chartType);
        }
    }
 // STYLING 
        private void styleBarChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(79, 129, 189));
    }
    private void stylePieChart(JFreeChart chart) {
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setLabelBackgroundPaint(Color.WHITE);
    }
    public ChartPanel getChartPanel(JFreeChart chart) {
        return new ChartPanel(chart);
    }
    public void exportChartAsPNG(JFreeChart chart, String filePath, int width, int height) throws IOException {
        ChartUtilities.saveChartAsPNG(new File(filePath), chart, width, height);
    }
//  ANALYSIS 
    public double getAverage(List<Double> values) {
        double sum = 0;
        for (double v : values) sum += v;
        return values.isEmpty() ? 0 : sum / values.size();
    }
    public double getMax(List<Double> values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values) if (v > max) max = v;
        return max;
    }
    public double getMin(List<Double> values) {
        double min = Double.POSITIVE_INFINITY;
        for (double v : values) if (v < min) min = v;
        return min;
    }
    public double getTotal(List<Double> values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum;
    }
}
