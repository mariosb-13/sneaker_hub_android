package es.iescarrillo.sneakerhub.models;

public class User {
    private String uid;
    private String username;
    private String email;
    private String phone;

    // NUEVOS CAMPOS
    private String profileImageUrl;
    private String zipCode;
    private String city;
    private String street;
    private String door;

    // Constructor vacío obligatorio para Firebase
    public User() {
    }

    public User(String uid, String username, String email, String phone) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    // Getters y Setters originales
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getDoor() {
        return door;
    }

    public void setDoor(String door) {
        this.door = door;
    }
}