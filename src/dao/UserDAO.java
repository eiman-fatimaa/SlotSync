package dao;

import model.*;
import java.sql.*;
//handles all db quereis relaetd to users (students and professors)
public class UserDAO {
    //fetches a user from db by email and returns their role - students/prof obj
    public User getUserByEmail(String email) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection failed");
            return null;
        }
        //query to fetch user details along with role-specific info (year for students, department for professors)
        String query = """
            SELECT ue.user_id, ue.email, ud.password_hash, ud.role,
                   ud.first_name, ud.last_name, ud.phone_number,
                   s.year, pd.department
            FROM user_email ue
            JOIN user_details ud ON ue.user_id = ud.user_id
            LEFT JOIN student s ON ue.user_id = s.student_id
            LEFT JOIN professor_department pd ON ue.user_id = pd.professor_id
            WHERE ue.email = ?
        """;

        try (conn;PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("user_id");
                String pass = rs.getString("password_hash");
                String role = rs.getString("role");

                String fname = rs.getString("first_name");
                String lname = rs.getString("last_name");
                String phone = rs.getString("phone_number");
                //returns a student or professor object based on the role, with all relevant details populated
                if (role.equals("STUDENT")) {
                    int year = rs.getInt("year");
                    return new Student(id, email, pass, fname, lname, phone, year);
                } else {
                    String dept = rs.getString("department");
                    return new Professor(id, email, pass, fname, lname, phone, dept);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    //fetches user from db  by their id and retirn stident or prof obj
    public User getUserById(int userId) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection failed");
            return null;
        }
        //similar to email query- query to fetch user details along with role-specific info (year for students, department for professors) based on user id
        String query = """
            SELECT ue.user_id, ue.email, ud.password_hash, ud.role,
                   ud.first_name, ud.last_name, ud.phone_number,
                   s.year, pd.department
            FROM user_email ue
            JOIN user_details ud ON ue.user_id = ud.user_id
            LEFT JOIN student s ON ue.user_id = s.student_id
            LEFT JOIN professor_department pd ON ue.user_id = pd.professor_id
            WHERE ue.user_id = ?
        """;

        try (conn;PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("user_id");
                String email = rs.getString("email");
                String pass = rs.getString("password_hash");
                String role = rs.getString("role");

                String fname = rs.getString("first_name");
                String lname = rs.getString("last_name");
                String phone = rs.getString("phone_number");
                //returns correct subclass based on role
                if (role.equals("STUDENT")) {
                    int year = rs.getInt("year");
                    return new Student(id, email, pass, fname, lname, phone, year);
                } else {
                    String dept = rs.getString("department");
                    return new Professor(id, email, pass, fname, lname, phone, dept);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}