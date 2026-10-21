package db;
import java.sql.*;

public class Db_connection {
	//Establishing the database connection
	private static final String url=
	        "jdbc:sqlserver://localhost:1433;databaseName=data_viz;encrypt=false;trustServerCertificate=true;";
	    private static final String user= "datavizapp";
	    private static final String password= "ASDFGHJKL345";

	    public static Connection getConnection() throws SQLException {
	        return DriverManager.getConnection(url,user,password);
	    }
}
