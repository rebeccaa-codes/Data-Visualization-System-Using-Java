package main;
import db.Db_connection;
import model.Dataset;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dataset_main {
	//Creates new dataset, returns the generated id
    public int insertDataset(String name, String description) throws SQLException {
        String sql= "INSERT INTO datasets(name, description) VALUES(?, ?)";
        try (Connection conn= Db_connection.getConnection();
             PreparedStatement stmt= conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) 
                	return keys.getInt(1);
            }
        }
        return -1;
    }

    //Get all saved datasets for the "reopen project" list
    public List<Dataset> getAllDatasets() throws SQLException {
        List<Dataset> results= new ArrayList<>();
        String sql= "SELECT id, name, description FROM datasets ORDER BY created_at DESC";

        try (Connection conn= Db_connection.getConnection();
             Statement stmt= conn.createStatement();
             ResultSet rs= stmt.executeQuery(sql)) {

            while (rs.next()) {
                results.add(new Dataset(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description")
                ));
            }
        }
        return results;
    }
}

