package model;

public class Student extends User {
    private int year;

    public Student(int userId, String email, String password,
                   String firstName, String lastName,
                   String phone, int year) {
        super(userId, email, password, firstName, lastName, phone);
        this.year = year;
    }

    public int getYear() { return year; }
}