package es.iescarrillo.sneakerhub.models;

public class CartItem {
    private String productId;
    private String modelName;
    private String subName; // Ej: "Off-White Lote 34"
    private double price;
    private String imageUrl;
    private int quantity;
    private String size;

    // Constructor vacío necesario para Firebase
    public CartItem() {}

    public CartItem(String productId, String modelName, String subName, double price, String imageUrl, String size) {
        this.productId = productId;
        this.modelName = modelName;
        this.subName = subName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.size = size;
        this.quantity = 1; // Por defecto empezamos con 1
    }

    // Getters
    public String getProductId() { return productId; }
    public String getModelName() { return modelName; }
    public String getSubName() { return subName; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getQuantity() { return quantity; }
    public String getSize() { return size; }

    // Setters
    public void setQuantity(int quantity) { this.quantity = quantity; }
    // (Puedes añadir el resto de setters si los necesitas en el futuro)
}