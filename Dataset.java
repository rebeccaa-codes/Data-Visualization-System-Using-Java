package model;

public class Dataset {
	// demonstrates encapsulation, cant be changed outside the class
	private int id;
    private String name;
    private String description;
    
   //Parameterized constructor
    public Dataset(int id,String name,String description) {
        this.id= id;
        this.name= name;
        this.description= description;
    }

    public int getId() { 
    	return id; 
    }
    
    public String getName() { 
    	return name; 
    }
    
    public String getDescription() { 
    	return description; 
    }

    // ADDED: without this, showing a Dataset in a JComboBox/JOptionPane
    // picker prints something like "model.Dataset@1a2b3c" instead of the
    // dataset's actual name.
    @Override
    public String toString() {
        return name;
    }
}
