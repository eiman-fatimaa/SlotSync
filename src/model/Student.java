package model;
//represents student user inheriting from user class
public class Student extends User {
    private int year;//academic yr of student 1-4

    //constructor
    public Student(int userId, String email, String password,
                   String firstName, String lastName,
                   String phone, int year) {
        super(userId, email, password, firstName, lastName, phone);
        this.year = year;
    }

    //getter
    public int getYear() { return year; }
}