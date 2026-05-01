package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Professor;
import model.Student;
import model.User;
import main.Main;
import view.StudentView;
import view.ProfessorView;
import service.AuthService;

public class LoginView {

    public static Scene getScene() {

        // ── TITLE ──────────────────────────────────────
        Label title = new Label("Appointment System");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // ── EMAIL ──────────────────────────────────────
        Label emailLabel = new Label("Email:");
        emailLabel.setFont(Font.font("Arial", 14));

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setPrefWidth(250);

        // ── PASSWORD ───────────────────────────────────
        Label passwordLabel = new Label("Password:");
        passwordLabel.setFont(Font.font("Arial", 14));

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefWidth(250);

        // ── ERROR LABEL ────────────────────────────────
        Label errorLabel = new Label("");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", 13));
        errorLabel.setVisible(false);   // hidden by default

        // ── LOGIN BUTTON ───────────────────────────────
        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(150);
        loginButton.setPrefHeight(35);
        loginButton.setFont(Font.font("Arial", 13));

        // ── BUTTON LOGIC ───────────────────────────────
        loginButton.setOnAction(e -> {

            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            // basic empty check
            if (email.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter email and password");
                errorLabel.setVisible(true);
                return;
            }

            // call AuthService
            AuthService authService = new AuthService();
            User user = authService.login(email, password);

            if (user == null) {
                // wrong credentials
                errorLabel.setText("Invalid email or password");
                errorLabel.setVisible(true);
                return;
            }

            // store the logged in user in Main
            Main.setCurrentUser(user);

            // switch to correct dashboard
            if (user instanceof Student) {
                Main.switchScene(StudentView.getScene((Student) user));
            } else if (user instanceof Professor) {
                Main.switchScene(ProfessorView.getScene((Professor) user));
            }
        });

        // ── LAYOUT ─────────────────────────────────────
        VBox layout = new VBox(10);   // 10px spacing between elements
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));  // 20px padding all sides

        layout.getChildren().addAll(
            title,
            emailLabel,
            emailField,
            passwordLabel,
            passwordField,
            loginButton,
            errorLabel
        );

        // login scene is smaller than main window
        return new Scene(layout, 400, 350);
    }
}