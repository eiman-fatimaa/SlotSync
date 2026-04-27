import dao.UserDAO;
import model.User;

public class Main {
    public static void main(String[] args) {

        UserDAO dao = new UserDAO();

        User user = dao.getUserByEmail("raza.ali@seecs.edu.pk");

        if (user != null) {
            System.out.println(user.getName());
            System.out.println(user.getRole());
        } else {
            System.out.println("Not found");
        }
    }
}



