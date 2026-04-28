package model;

public abstract class User {
    protected int userId;
    protected String email;
    protected String password;
    protected String firstName;
    protected String lastName;
    protected String phone;

    public User(int userId, String email, String password,
                String firstName, String lastName, String phone) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public int getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}