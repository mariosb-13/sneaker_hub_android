package es.iescarrillo.sneakerhub.models;

import java.util.List;

public class Order {
    private String order_id;
    private long order_date;
    private String status;
    private double total;
    private List<SneakerCopy> purchased_sneakers;

    // Campos de envío y pago
    private String address;
    private String city;
    private String zipCode;
    private String door;
    private String paymentMethod;

    public Order() {
    }

    // Método para mostrar la lista de zapatillas en el historial
    public String getItemsListFormatted() {
        if (purchased_sneakers == null || purchased_sneakers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < purchased_sneakers.size(); i++) {
            sb.append("• ").append(purchased_sneakers.get(i).getName_snap());
            if (i < purchased_sneakers.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    // Getters y Setters
    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public long getOrder_date() {
        return order_date;
    }

    public void setOrder_date(long order_date) {
        this.order_date = order_date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<SneakerCopy> getPurchased_sneakers() {
        return purchased_sneakers;
    }

    public void setPurchased_sneakers(List<SneakerCopy> purchased_sneakers) {
        this.purchased_sneakers = purchased_sneakers;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getDoor() {
        return door;
    }

    public void setDoor(String door) {
        this.door = door;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}