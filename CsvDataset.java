package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// RENAMED from "Dataset" to "CsvDataset": the project already has a
// model.Dataset (the DB record for the datasets table — id/name/description).
// That's a completely different thing from this class (an in-memory
// parsed-CSV container), but Java won't let a file import two different
// classes both simply named "Dataset". This one also had no package
// statement originally, which meant classes in named packages (like
// ui.Main) couldn't import it at all — added "package parser;" to fix that.
public class CsvDataset {

    public enum ColumnType {
        NUMERIC, TEXT
    }

    private final List<String> headers;
    private final List<ColumnType> columnTypes;
    private final List<Map<String, Object>> rows;

    public CsvDataset(List<String> headers, List<ColumnType> columnTypes) {
        this.headers = headers;
        this.columnTypes = columnTypes;
        this.rows = new ArrayList<>();
    }

    public void addRow(Map<String, Object> row) {
        this.rows.add(row);
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<ColumnType> getColumnTypes() {
        return columnTypes;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return headers.size();
    }

    public void printSchema() {
        System.out.println("=== Dataset Schema Discovery ===");
        System.out.println("Total Columns Discovered: " + getColumnCount());
        for (int i = 0; i < headers.size(); i++) {
            System.out.println(" - Column " + (i + 1) + ": '" + headers.get(i) + "' [" + columnTypes.get(i) + "]");
        }
        System.out.println("Total Valid Rows Loaded : " + getRowCount());
        System.out.println("================================\n");
    }
}
