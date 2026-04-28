package service;

import dao.UserDAO;
import model.User;

public class AuthService {

    private UserDAO userDAO = new UserDAO();

    public User login(String email, String password) {

        User user = userDAO.getUserByEmail(email);

        if (user == null) {
            System.out.println("User not found");
            return null;
        }

        if (!user.getPassword().equals(password)) {
            System.out.println("Invalid password");
            return null;
        }

        System.out.println("Login successful");
        return user;
    }
}