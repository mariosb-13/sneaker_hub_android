package es.iescarrillo.sneakerhub.models;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private String rol;

    public User() {
        this.rol = "cliente"; // Por defecto siempre cliente
    }

    public User(String uid, String fullName, String email, String phone) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.rol = "cliente";
    }

    // Getters y Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}