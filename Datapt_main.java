package main;
import db.Db_connection;
import model.Datapts;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Datapt_main {

    // Insert many rows at once (used when importing a CSV)
    // FIX: was inserting into "data_pts" while getByDataset() below reads
    // from "data_points" — these must be the same table. Changed both to
    // "data_points". If your real schema actually uses "data_pts", flip
    // this line back instead and keep getByDataset() consistent with it.
    public void insertBatch(int datasetId, List<Datapts> points) throws SQLException {
        String sql = "INSERT INTO data_points (dataset_id, category, series_name, value, row_order) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn= Db_connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Datapts p : points) {
                stmt.setInt(1, datasetId);
                stmt.setString(2, p.getCategory());
                stmt.setString(3, p.getSeriesName());
                stmt.setDouble(4, p.getValue());
                stmt.setInt(5, p.getRowOrder());
                stmt.addBatch();
            }
            stmt.executeBatch();   // sends all inserts in one trip — much faster than one-by-one
        }
    }

    // Get all data points for a dataset (used to feed the chart)
    public List<Datapts> getByDataset(int datasetId) throws SQLException {
        List<Datapts> results = new ArrayList<>();
        String sql = "SELECT * FROM data_points WHERE dataset_id = ? ORDER BY row_order";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, datasetId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Datapts(
                        rs.getInt("id"),
                        rs.getInt("dataset_id"),
                        rs.getString("category"),
                        rs.getString("series_name"),
                        rs.getDouble("value"),
                        rs.getInt("row_order")
                    ));
                }
            }
        }
        return results;
    }
}
