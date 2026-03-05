package es.iescarrillo.sneakerhub.models;

import java.util.List; // <--- IMPORTANTE

public class Sneaker {

    private String name;
    private String brand;
    private String gender;
    private Double price;
    private String imageUrl;
    private List<String> sizes; // <--- NUEVO CAMPO

    // 1. Constructor vacío (Obligatorio Firebase)
    public Sneaker() { }

    // 2. Constructor lleno
    public Sneaker(String name, String brand, String gender, Double price, String imageUrl, List<String> sizes) {
        this.name = name;
        this.brand = brand;
        this.gender = gender;
        this.price = price;
        this.imageUrl = imageUrl;
        this.sizes = sizes;
    }

    // 3. Getters y Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getSizes() { return sizes; } // <--- NUEVO GETTER
    public void setSizes(List<String> sizes) { this.sizes = sizes; } // <--- NUEVO SETTER
}