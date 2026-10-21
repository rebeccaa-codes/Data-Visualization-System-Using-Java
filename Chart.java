package model;
import java.sql.Timestamp;

public class Chart {
	private int id;
	private int datasetId;
	private String chartType;
	private String title;
	private Timestamp createdAt;
	
	public Chart(int id, int datasetId, String chartType, String title, Timestamp createdAt) {
		this.id= id;
		this.datasetId= datasetId;
		this.chartType= chartType;
		this.title= title;
		this.createdAt= createdAt;
	}
	
	public int getId() { 
		return id; 
	}
	
    public int getDatasetId() { 
    	return datasetId; 
    }
    
    public String getChartType() { 
		return chartType; 
	}
	
    public String getTitle() { 
    	return title; 
    }
    
    public Timestamp getCreatedAt() {
    	return createdAt;
    }
    
}
