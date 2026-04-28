package model;

public class Professor extends User {
    private String department;

    public Professor(int userId, String email, String password,
                     String firstName, String lastName,
                     String phone, String department) {
        super(userId, email, password, firstName, lastName, phone);
        this.department = department;
    }

    public String getDepartment() { return department; }
}