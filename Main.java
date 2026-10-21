package ui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import main.Chart_main;
import main.Datapt_main;
import main.Dataset_main;
import model.Dataset;
import model.Datapts;
import parser.CsvDataset;
import parser.UniversalDataEngine;

public class Main {

    static ChartHelper chartHelper = new ChartHelper();

    // ===== DB HOOKS =====
    static Dataset_main datasetDAO = new Dataset_main();
    static Datapt_main datapointDAO = new Datapt_main();
    static Chart_main chartDAO = new Chart_main();

    // -1 = nothing saved/loaded yet. Set after a successful Save or Load.
    static int currentDatasetId = -1;

    static JPanel chartContainer;
    static JLabel statsLabel;
    static JFrame frame;
    static JFreeChart currentChart;
    static String currentType = "BAR";

    // The parsed CSV, once imported. Replaces the old raw String[][] approach -
    // this holds real column types (NUMERIC/TEXT) so we know which columns
    // are safe to chart as values.
    static CsvDataset currentDataset;

    // Data actually being charted right now
    static List<String> categories = new ArrayList<>(Arrays.asList("Jan", "Feb", "Mar", "Apr", "May"));
    static List<Double> values = new ArrayList<>(Arrays.asList(120.0, 200.0, 150.0, 80.0, 220.0));

    static JComboBox<String> labelColumnDropdown;
    static JComboBox<String> valueColumnDropdown;

    public static void main(String[] args) {
        frame = new JFrame("Data Visualization Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 700);
        frame.setLayout(new BorderLayout());

        chartContainer = new JPanel(new BorderLayout());
        frame.add(chartContainer, BorderLayout.CENTER);

        // chart type buttons ----
        JPanel topPanel = new JPanel();
        JButton barBtn = new JButton("Bar Chart");
        JButton lineBtn = new JButton("Line Chart");
        JButton pieBtn = new JButton("Pie Chart");
        JButton scatterBtn = new JButton("Scatter Chart");

        barBtn.addActionListener(e -> { currentType = "BAR"; showChart(); });
        lineBtn.addActionListener(e -> { currentType = "LINE"; showChart(); });
        pieBtn.addActionListener(e -> { currentType = "PIE"; showChart(); });
        scatterBtn.addActionListener(e -> { currentType = "SCATTER"; showChart(); });

        topPanel.add(barBtn);
        topPanel.add(lineBtn);
        topPanel.add(pieBtn);
        topPanel.add(scatterBtn);
        frame.add(topPanel, BorderLayout.NORTH);

        // Import/Export + DB + Column pickers + Stats ----
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JPanel fileButtons = new JPanel();
        JButton importBtn = new JButton("Import CSV");
        JButton exportBtn = new JButton("Export Chart as PNG");
        importBtn.addActionListener(e -> importCSV());
        exportBtn.addActionListener(e -> exportPNG());
        fileButtons.add(importBtn);
        fileButtons.add(exportBtn);

        JButton saveDbBtn = new JButton("Save to Database");
        JButton loadDbBtn = new JButton("Load Saved Dataset");
        saveDbBtn.addActionListener(e -> saveToDatabase());
        loadDbBtn.addActionListener(e -> loadFromDatabase());
        fileButtons.add(saveDbBtn);
        fileButtons.add(loadDbBtn);

        // Column selectors
        JPanel columnPanel = new JPanel();
        columnPanel.add(new JLabel("Label Column:"));
        labelColumnDropdown = new JComboBox<>(new String[]{"Category"});
        columnPanel.add(labelColumnDropdown);

        columnPanel.add(new JLabel("Value Column:"));
        valueColumnDropdown = new JComboBox<>(new String[]{"Value"});
        columnPanel.add(valueColumnDropdown);

        JButton applyBtn = new JButton("Apply Columns");
        applyBtn.addActionListener(e -> applyColumnSelection());
        columnPanel.add(applyBtn);

        statsLabel = new JLabel("Stats will appear here", SwingConstants.CENTER);

        bottomPanel.add(fileButtons);
        bottomPanel.add(columnPanel);
        bottomPanel.add(statsLabel);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        showChart(); // default sample data
        frame.setVisible(true);
    }

    // ===== IMPORT (now via UniversalDataEngine, not ChartHelper) =====
    static void importCSV() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selectedFile = fileChooser.getSelectedFile();
        CsvDataset parsed = UniversalDataEngine.parseAndCleanCsv(selectedFile.getAbsolutePath());

        if (parsed == null || parsed.getRowCount() == 0) {
            JOptionPane.showMessageDialog(frame,
                    "Could not parse any valid rows from that file.",
                    "Import Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentDataset = parsed;
        currentDatasetId = -1; // fresh import, not yet saved to DB

        // Refresh column dropdowns with the REAL column names from this file
        labelColumnDropdown.removeAllItems();
        valueColumnDropdown.removeAllItems();
        for (String h : currentDataset.getHeaders()) {
            labelColumnDropdown.addItem(h);
            valueColumnDropdown.addItem(h);
        }

        // Auto-pick sensible defaults: first TEXT column as label, first NUMERIC column as value
        List<CsvDataset.ColumnType> types = currentDataset.getColumnTypes();
        int autoLabelCol = 0;
        int autoValueCol = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i) == CsvDataset.ColumnType.TEXT) { autoLabelCol = i; break; }
        }
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i) == CsvDataset.ColumnType.NUMERIC) { autoValueCol = i; break; }
        }
        labelColumnDropdown.setSelectedIndex(autoLabelCol);
        valueColumnDropdown.setSelectedIndex(autoValueCol);

        applyColumnSelection();

        String skippedNote = UniversalDataEngine.lastSkippedRowCount > 0
                ? "\n" + UniversalDataEngine.lastSkippedRowCount + " row(s) skipped (column count mismatch)."
                : "";
        JOptionPane.showMessageDialog(frame,
                "CSV imported! " + currentDataset.getRowCount() + " rows, " +
                currentDataset.getColumnCount() + " columns found.\n" +
                "Columns: " + String.join(", ", currentDataset.getHeaders()) + skippedNote);
    }

    // ===== APPLY COLUMNS (now reads from CsvDataset's typed rows) =====
    static void applyColumnSelection() {
        if (currentDataset == null) {
            JOptionPane.showMessageDialog(frame, "No CSV loaded yet - import a file first.");
            return;
        }

        String labelCol = (String) labelColumnDropdown.getSelectedItem();
        String valueCol = (String) valueColumnDropdown.getSelectedItem();
        if (labelCol == null || valueCol == null) return;

        if (labelCol.equals(valueCol)) {
            JOptionPane.showMessageDialog(frame,
                    "Label Column and Value Column can't be the same column.",
                    "Invalid selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int valueColIndex = currentDataset.getHeaders().indexOf(valueCol);
        if (valueColIndex >= 0 && currentDataset.getColumnTypes().get(valueColIndex) != CsvDataset.ColumnType.NUMERIC) {
            JOptionPane.showMessageDialog(frame,
                    "Warning: selected Value column is not fully numeric. Non-numeric cells will show as 0.",
                    "Data Type Notice", JOptionPane.WARNING_MESSAGE);
        }

        categories = new ArrayList<>();
        values = new ArrayList<>();
        for (Map<String, Object> row : currentDataset.getRows()) {
            Object labelVal = row.get(labelCol);
            categories.add(labelVal == null ? "" : String.valueOf(labelVal));

            Object valVal = row.get(valueCol);
            double v = 0.0;
            if (valVal instanceof Double) {
                v = (Double) valVal;
            } else if (valVal != null) {
                try { v = Double.parseDouble(String.valueOf(valVal)); } catch (NumberFormatException ignored) { }
            }
            values.add(v);
        }

        showChart();
    }

    // EXPORT
    static void exportPNG() {
        if (currentChart == null) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("chart.png"));
        int result = fileChooser.showSaveDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try {
                chartHelper.exportChartAsPNG(currentChart, saveFile.getAbsolutePath(), 800, 600);
                JOptionPane.showMessageDialog(frame, "Chart exported to:\n" + saveFile.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error saving PNG: " + ex.getMessage(),
                        "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ===== SAVE TO DATABASE =====
    // Saves whatever is CURRENTLY CHARTED (categories/values) as a new
    // dataset + its data points + a record of this chart. Always INSERTS a
    // new dataset - running Save twice creates two separate datasets.
    static void saveToDatabase() {
        if (categories.isEmpty() || values.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No data to save - import a CSV first.");
            return;
        }

        String name = JOptionPane.showInputDialog(frame, "Dataset name:");
        if (name == null || name.trim().isEmpty()) return;

        String description = JOptionPane.showInputDialog(frame, "Description (optional):");
        if (description == null) description = "";

        try {
            int datasetId = datasetDAO.insertDataset(name.trim(), description.trim());
            if (datasetId == -1) {
                JOptionPane.showMessageDialog(frame, "Failed to create dataset.",
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // seriesName hardcoded to "Data" - matches the series name
            // ChartHelper uses internally. Change if you add multi-series support.
            List<Datapts> points = new ArrayList<>();
            for (int i = 0; i < categories.size(); i++) {
                points.add(new Datapts(0, datasetId, categories.get(i), "Data", values.get(i), i));
            }
            datapointDAO.insertBatch(datasetId, points);

            chartDAO.insertChart(datasetId, currentType, name.trim() + " (" + currentType + ")");

            currentDatasetId = datasetId;

            JOptionPane.showMessageDialog(frame,
                    "Saved dataset \"" + name.trim() + "\" with " + points.size() + " rows.");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== LOAD FROM DATABASE =====
    static void loadFromDatabase() {
        try {
            List<Dataset> datasets = datasetDAO.getAllDatasets();
            if (datasets.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No saved datasets yet.");
                return;
            }

            Dataset[] options = datasets.toArray(new Dataset[0]);
            Dataset chosen = (Dataset) JOptionPane.showInputDialog(
                    frame,
                    "Choose a dataset to load:",
                    "Load Saved Dataset",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (chosen == null) return;

            List<Datapts> points = datapointDAO.getByDataset(chosen.getId());
            if (points.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "That dataset has no data points.",
                        "Nothing to load", JOptionPane.WARNING_MESSAGE);
                return;
            }

            categories = new ArrayList<>();
            values = new ArrayList<>();
            for (Datapts p : points) {
                categories.add(p.getCategory());
                values.add(p.getValue());
            }

            currentDatasetId = chosen.getId();
            currentDataset = null; // this data came from the DB, not a fresh CSV parse

            showChart();
            JOptionPane.showMessageDialog(frame,
                    "Loaded \"" + chosen.getName() + "\" - " + points.size() + " rows.");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Load Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // DISPLAY CHART + STATS
    static void showChart() {
        if (categories.isEmpty() || values.isEmpty()) {
            statsLabel.setText("No data loaded. Import a CSV first.");
            return;
        }
        if (currentType.equals("SCATTER")) {
            List<Double> xVals = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) xVals.add((double) (i + 1));
            currentChart = chartHelper.createScatterChart(
                    chartHelper.createXYDataset(xVals, values, "Data"),
                    "Scatter Chart", "Index", "Value"
            );
        } else {
            currentChart = chartHelper.getChart(currentType, categories, values);
        }
        ChartPanel chartPanel = chartHelper.getChartPanel(currentChart);
        chartContainer.removeAll();
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();

        double avg = chartHelper.getAverage(values);
        double max = chartHelper.getMax(values);
        double min = chartHelper.getMin(values);
        double total = chartHelper.getTotal(values);

        statsLabel.setText(String.format(
                "Rows: %d | Average: %.2f | Max: %.2f | Min: %.2f | Total: %.2f",
                values.size(), avg, max, min, total
        ));
    }
}
