package service;

import dao.UserDAO;
import model.User;
//handles user authentication - verfies email and password at login
public class AuthService {
    //dao used to look up user from db
    private UserDAO userDAO = new UserDAO();
    //checks email and password and returns  user obj if valid
    public User login(String email, String password) {
        //looks up user by email
        User user = userDAO.getUserByEmail(email);

        if (user == null) {
            System.out.println("User not found");
            return null;
        }
        //compares entered password with stored password
        if (!user.getPassword().equals(password)) {
            System.out.println("Invalid password");
            return null;
        }

        System.out.println("Login successful");
        return user;
    }
}