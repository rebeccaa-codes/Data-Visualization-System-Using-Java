package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// FIXED: added "package parser;" (was default/unnamed package — couldn't
// be imported by ui.Main otherwise) and rewrote type inference to be
// two-pass (see comment further down) to fix a bug where a blank cell in
// row 1 would permanently misclassify that column as TEXT.
public class UniversalDataEngine {

    // Set after each parseAndCleanCsv() call so the UI can tell the user
    // how many rows were rejected. A cleaner design would return a small
    // ParseResult{dataset, skippedRows} object instead of using a static
    // field, but this keeps the original method signature intact.
    public static int lastSkippedRowCount = 0;

    public static CsvDataset parseAndCleanCsv(String filePath) {
        List<String> headers = new ArrayList<>();
        List<List<String>> rawDataRows = new ArrayList<>();
        int skippedRows = 0;
        int expectedColumnCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            System.out.println("--- Starting Universal Data Processing Engine ---\n");

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] rawCells = line.split(",", -1);

                if (lineNumber == 1) {
                    expectedColumnCount = rawCells.length;
                    for (String cell : rawCells) headers.add(cell.trim());
                    continue;
                }

                if (rawCells.length != expectedColumnCount) {
                    System.out.println("Row " + lineNumber + " SKIPPED: Column count mismatch.");
                    skippedRows++;
                    continue;
                }

                List<String> trimmedCells = new ArrayList<>();
                for (String cell : rawCells) trimmedCells.add(cell.trim());
                rawDataRows.add(trimmedCells);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            lastSkippedRowCount = skippedRows;
            return null;
        }

        if (headers.isEmpty()) {
            lastSkippedRowCount = skippedRows;
            return null; // empty file
        }

        // FIX: two-pass type inference. Original code only ever looked at
        // ROW 1 of column i to decide that column's type — and since blank
        // cells get imputed to "N/A" before type inference ran, a blank in
        // row 1 permanently locked that column in as TEXT even if every
        // other row was numeric. This scans down each column across ALL
        // rows for the first non-empty value instead.
        List<CsvDataset.ColumnType> columnTypes = new ArrayList<>();
        for (int col = 0; col < expectedColumnCount; col++) {
            CsvDataset.ColumnType inferred = CsvDataset.ColumnType.TEXT; // default if column is entirely empty
            for (List<String> row : rawDataRows) {
                String cell = row.get(col);
                if (!cell.isEmpty()) {
                    inferred = isNumeric(cell) ? CsvDataset.ColumnType.NUMERIC : CsvDataset.ColumnType.TEXT;
                    break;
                }
            }
            columnTypes.add(inferred);
        }

        CsvDataset dataset = new CsvDataset(headers, columnTypes);

        for (List<String> row : rawDataRows) {
            Map<String, Object> rowMap = new HashMap<>();
            for (int i = 0; i < expectedColumnCount; i++) {
                String cell = row.get(i);
                boolean isNumericCol = columnTypes.get(i) == CsvDataset.ColumnType.NUMERIC;

                if (cell.isEmpty()) {
                    rowMap.put(headers.get(i), isNumericCol ? 0.0 : "N/A");
                    continue;
                }

                if (isNumericCol) {
                    try {
                        rowMap.put(headers.get(i), Double.parseDouble(cell));
                    } catch (NumberFormatException e) {
                        // A cell that doesn't fit the column's inferred type -
                        // kept as text rather than crashing. Downstream code
                        // reading this "numeric" column must handle the
                        // possibility of a String showing up here.
                        rowMap.put(headers.get(i), cell);
                        System.out.println("Couldn't convert \"" + cell + "\" to a number, kept as text.");
                    }
                } else {
                    rowMap.put(headers.get(i), cell);
                }
            }
            dataset.addRow(rowMap);
        }

        System.out.println("Processing Complete. Total Skipped Rows: " + skippedRows + "\n");
        lastSkippedRowCount = skippedRows;
        return dataset;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Standalone test harness - not part of the app's real entry point
    // (ui.Main is). Safe to leave as-is; just don't run this expecting it
    // to launch the Swing UI.
    public static void main(String[] args) {
        String filePath = "data.csv";
        CsvDataset dataset = parseAndCleanCsv(filePath);
        if (dataset != null) {
            dataset.printSchema();
            System.out.println("=== Ingested Row Preview ===");
            dataset.getRows().forEach(System.out::println);
        } else {
            System.out.println("Engine failed to parse a valid dataset.");
        }
    }
}
