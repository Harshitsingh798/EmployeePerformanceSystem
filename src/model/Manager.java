package model;

/**
 * Manager class - extends User (Inheritance)
 */
public class Manager extends User {

    private String department;

    public Manager() {
        super();
        this.role = "MANAGER";
    }

    public Manager(int userId, String name, String email, String password, String department) {
        super(userId, name, email, password, "MANAGER");
        this.department = department;
    }

    @Override
    public String getDashboardType() {
        return "Manager Dashboard";
    }

    @Override
    public void displayInfo() {
        System.out.println("Manager: " + name + " | Dept: " + department);
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public String toString() {
        return "Manager{" + super.toString() + ", department='" + department + "'}";
    }
}
