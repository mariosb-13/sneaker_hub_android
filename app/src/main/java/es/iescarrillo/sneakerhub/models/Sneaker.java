package es.iescarrillo.sneakerhub.models;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;
import java.util.List;

public class Sneaker implements Serializable {
    private String id;

    private String brand;
    private String gender;
    private String name;
    private double price;
    private List<String> sizes;

    // Novedades
    private String imageUrl;
    private boolean isTrending;
    private List<String> images360; // El array con las 36 fotos

    // Constructor vacío obligatorio para Firebase
    public Sneaker() {}

    public Sneaker(String id, String brand, String gender, String name, double price, List<String> sizes, String imageUrl, boolean isTrending, List<String> images360) {
        this.id = id;
        this.brand = brand;
        this.gender = gender;
        this.name = name;
        this.price = price;
        this.sizes = sizes;
        this.imageUrl = imageUrl;
        this.isTrending = isTrending;
        this.images360 = images360;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public List<String> getSizes() { return sizes; }
    public void setSizes(List<String> sizes) { this.sizes = sizes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("isTrending")
    public boolean isTrending() { return isTrending; }

    @PropertyName("isTrending")
    public void setTrending(boolean trending) { isTrending = trending; }

    public List<String> getImages360() { return images360; }
    public void setImages360(List<String> images360) { this.images360 = images360; }
}