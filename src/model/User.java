package model;

/**
 * Abstract base class for all users - demonstrates Inheritance & Abstraction
 */
public abstract class User {
    protected int userId;
    protected String name;
    protected String email;
    protected String password;
    protected String role; // MANAGER or EMPLOYEE

    public User() {}

    public User(int userId, String name, String email, String password, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Abstract method - Polymorphism
    public abstract String getDashboardType();
    public abstract void displayInfo();

    // Getters & Setters (Encapsulation)
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "User{id=" + userId + ", name='" + name + "', role=" + role + "}";
    }
}
