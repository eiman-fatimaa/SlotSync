package model;
//abstract base class for all user - student and prof extend this
public abstract class User {
    //shared fields for all user types
    protected int userId;
    protected String email;
    protected String password;
    protected String firstName;
    protected String lastName;
    protected String phone;

    //constructor for user
    public User(int userId, String email, String password,
                String firstName, String lastName, String phone) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    //getters
    public int getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    //returns roles based on which class obj is from
    public String getRole(){
        if (this instanceof Student) return "STUDENT";
        if (this instanceof Professor) return "PROFESSOR";
        return "UNKNOWN";
    }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
}