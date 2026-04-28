import service.AuthService;
import model.User;
import util.TimetableParser;
import model.Timetable;

public class Main {

    public static void main(String[] args) {

        AuthService auth = new AuthService();

        // 🔹 Test login
        User user = auth.login("raza.ali@seecs.edu.pk", "ProfRz1");

        if (user != null) {
            System.out.println("Logged in as: " + user.getEmail());
        }

        // 🔹 Test timetable parsing
        Timetable timetable = TimetableParser.parseJSON(
                "timetable.json", 11);

        System.out.println("Entries parsed: " + timetable.getEntries().size());
    }
}