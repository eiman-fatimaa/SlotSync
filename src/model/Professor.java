package model;
//represents a prof user of the system, inherits from User
public class Professor extends User {
    private String department;//dpt prof belongs to

    //constructor
    public Professor(int userId, String email, String password,
                     String firstName, String lastName,
                     String phone, String department) {
        super(userId, email, password, firstName, lastName, phone);
        this.department = department;
    }

    //getter
    public String getDepartment() { return department; }
}