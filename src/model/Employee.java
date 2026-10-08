package model;

/**
 * Employee class - extends User (Inheritance)
 */
public class Employee extends User {

    private String department;
    private String designation;
    private int managerId;

    public Employee() {
        super();
        this.role = "EMPLOYEE";
    }

    public Employee(int userId, String name, String email, String password, String department, String designation, int managerId) {
        super(userId, name, email, password, "EMPLOYEE");
        this.department = department;
        this.designation = designation;
        this.managerId = managerId;
    }

    @Override
    public String getDashboardType() {
        return "Employee Dashboard";
    }

    @Override
    public void displayInfo() {
        System.out.println("Employee: " + name + " | Dept: " + department + " | Designation: " + designation);
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }

    @Override
    public String toString() {
        return "Employee{" + super.toString() + ", dept='" + department + "', designation='" + designation + "'}";
    }
}
