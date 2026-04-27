package es.iescarrillo.sneakerhub.models;

public class CartItem {
    private String productId;
    private String modelName;
    private String subName;
    private double price;
    private double originalPrice; // --- NUEVO: Para guardar el precio sin rebaja ---
    private String imageUrl;
    private int quantity;
    private String size;

    // Constructor vacío necesario para Firebase
    public CartItem() {}

    // --- NUEVO: Constructor actualizado con originalPrice ---
    public CartItem(String productId, String modelName, String subName, double price, double originalPrice, String imageUrl, String size) {
        this.productId = productId;
        this.modelName = modelName;
        this.subName = subName;
        this.price = price;
        this.originalPrice = originalPrice;
        this.imageUrl = imageUrl;
        this.size = size;
        this.quantity = 1; // Por defecto empezamos con 1
    }

    public String getProductId() { return productId; }
    public String getModelName() { return modelName; }
    public String getSubName() { return subName; }
    public double getPrice() { return price; }
    public double getOriginalPrice() { return originalPrice; } // Getter nuevo
    public String getImageUrl() { return imageUrl; }
    public int getQuantity() { return quantity; }
    public String getSize() { return size; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; } // Setter nuevo

    public void setProductId(String productId) { this.productId = productId; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public void setSubName(String subName) { this.subName = subName; }
    public void setPrice(double price) { this.price = price; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSize(String size) { this.size = size; }
}