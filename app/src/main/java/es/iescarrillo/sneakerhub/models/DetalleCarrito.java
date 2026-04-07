package es.iescarrillo.sneakerhub.models;

import java.io.Serializable;

public class DetalleCarrito implements Serializable {
    private String detalleCartId;
    private String productId;
    private String name;
    private String brand;
    private double price;
    private String imageUrl;
    private String tallaElegida;
    private int cantidad;

    public DetalleCarrito() {}

    public String getDetalleCartId() { return detalleCartId; }
    public void setDetalleCartId(String detalleCartId) { this.detalleCartId = detalleCartId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTallaElegida() { return tallaElegida; }
    public void setTallaElegida(String tallaElegida) { this.tallaElegida = tallaElegida; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}