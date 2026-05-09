package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.User;
import view.LoginView;

//manages a single window and scene switching
public class Main extends Application {

    // one single window shared by everyone
    private static Stage primaryStage;

    // the logged in user stored here so all views can access it
    private static User currentUser;
    //sets up the window and shows the login screen
    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Appointment System");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.setResizable(false);

        // start with login screen
        showLoginScreen();

        primaryStage.show();
    }

    // called by LoginView after successful login
    public static void switchScene(Scene newScene) {
        primaryStage.setScene(newScene);
    }

    // called at app start
    public static void showLoginScreen() {
        Scene loginScene = LoginView.getScene();
        primaryStage.setScene(loginScene);
    }

    // store logged in user
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    // any view can call this to get the logged in user
    public static User getCurrentUser() {
        return currentUser;
    }
    //used if a view needs direct stage access
    public static Stage getStage() {
        return primaryStage;
    }
    //main for javafx
    public static void main(String[] args) {
        launch(args);
    }
}