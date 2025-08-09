package PartB.SemanticSearch.model;

/** Simple POJO we store in Redis as a Hash (plus a BLOB vector field). */
public class RentalHouse {
    private String id;         // we'll use a generated UUID
    private String title;      // short title
    private String description; // main text used for embedding
    private String location;
    private double price;       // monthly USD


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title){ this.title = title; }
    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }
    public String getLocation(){ return location; }
    public void setLocation(String location){ this.location = location; }
    public double getPrice(){ return price; }
    public void setPrice(double price){ this.price = price; }
}
