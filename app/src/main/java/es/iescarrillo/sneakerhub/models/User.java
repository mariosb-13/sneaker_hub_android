package es.iescarrillo.sneakerhub.models;

public class User {
    private String uid;
    private String username;
    private String email;
    private String phone;

    // Constructor vacío obligatorio para Firebase
    public User() {}

    public User(String uid, String username, String email, String phone) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    // Getters y Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}