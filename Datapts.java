package model;

public class Datapts {
	private int id;
	private int datasetId;
	private String category;
	private String seriesName;
	private double value;
	private int rowOrder;
	
	public Datapts(int id,int datasetId,String category,String seriesName,double value,int rowOrder) {
		this.id= id;
		this.datasetId= datasetId;
		this.category= category;
		this.seriesName= seriesName;
		this.value= value;
		this.rowOrder= rowOrder;
	}
	
	public int getId() { 
    	return id; 
    }
	
	public int getDatasetId() { 
    	return datasetId; 
    }
	
	public String getCategory() { 
		return category; 
	}
	
    public String getSeriesName() { 
    	return seriesName; 
    }
    
    public double getValue() {
    	return value;
    }
    public int getRowOrder() { 
    	return rowOrder; 
    }

}
