package es.iescarrillo.sneakerhub.models;

public class SneakerCopy {
    private String copia_id;
    private String id_producto_original;
    private String name_snap;
    private String brand_snap;
    private String model_snap;
    private double price_snap;
    private String talla_elegida;
    private String imagen_snap;
    private int cantidad_comprada;

    public SneakerCopy() {}

    // Getters y Setters
    public String getCopia_id() { return copia_id; }
    public void setCopia_id(String copia_id) { this.copia_id = copia_id; }

    public String getId_producto_original() { return id_producto_original; }
    public void setId_producto_original(String id_producto_original) { this.id_producto_original = id_producto_original; }

    public String getName_snap() { return name_snap; }
    public void setName_snap(String name_snap) { this.name_snap = name_snap; }

    public String getBrand_snap() { return brand_snap; }
    public void setBrand_snap(String brand_snap) { this.brand_snap = brand_snap; }

    public String getModel_snap() { return model_snap; }
    public void setModel_snap(String model_snap) { this.model_snap = model_snap; }

    public double getPrice_snap() { return price_snap; }
    public void setPrice_snap(double price_snap) { this.price_snap = price_snap; }

    public String getTalla_elegida() { return talla_elegida; }
    public void setTalla_elegida(String talla_elegida) { this.talla_elegida = talla_elegida; }

    public String getImagen_snap() { return imagen_snap; }
    public void setImagen_snap(String imagen_snap) { this.imagen_snap = imagen_snap; }

    public int getCantidad_comprada() { return cantidad_comprada; }
    public void setCantidad_comprada(int cantidad_comprada) { this.cantidad_comprada = cantidad_comprada; }

    public double calcularSubtotal() {
        return price_snap * cantidad_comprada;
    }
}