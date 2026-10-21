package main;
import db.Db_connection;
import model.Chart;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Chart_main {
	//
    public int insertChart(int datasetId, String chartType, String title) throws SQLException {
        String sql= "INSERT INTO charts(dataset_id, chart_type, title) VALUES(?, ?, ?)";
        try (Connection conn= Db_connection.getConnection();
             PreparedStatement stmt= conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, datasetId);
            stmt.setString(2, chartType);
            stmt.setString(3, title);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) 
                	return keys.getInt(1);
            }
        }
        return -1;
    }

    public List<Chart> getChartsByDataset(int datasetId) throws SQLException {
        List<Chart> results = new ArrayList<>();
        String sql = "SELECT * FROM charts WHERE dataset_id = ? ORDER BY created_at DESC";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, datasetId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Chart(
                        rs.getInt("id"),
                        rs.getInt("dataset_id"),
                        rs.getString("chart_type"),
                        rs.getString("title"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return results;
    }
}

