package es.iescarrillo.sneakerhub.models;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Sneaker implements Serializable {

    private String id;
    private String name;
    private String brand;
    private String model;  // Añadido por tu captura
    private String gender; // Añadido por tu captura
    private double price;
    private String imageUrl;
    private Map<String, Integer> sizes;
    private boolean isTrending;
    private List<String> images360;

    public Sneaker() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Map<String, Integer> getSizes() { return sizes; }
    public void setSizes(Map<String, Integer> sizes) { this.sizes = sizes; }

    // Forzamos a Firebase a que lea exactamente "isTrending"
    @PropertyName("isTrending")
    public boolean isTrending() { return isTrending; }

    @PropertyName("isTrending")
    public void setTrending(boolean isTrending) { this.isTrending = isTrending; }

    public List<String> getImages360() { return images360; }
    public void setImages360(List<String> images360) { this.images360 = images360; }
}