package view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import main.Main;
import model.Professor;
import model.Student;
import model.User;
import service.AuthService;

/**
 * ══════════════════════════════════════════════════════════════════════
 * LoginView — "Obsidian Glass" dark-theme login screen
 *
 * Design language  : Deep navy gradients + frosted-glass card +
 *                    electric-indigo accents
 * Animation        : Smooth fade + scale-up card entrance;
 *                    slide-down error messages
 * CSS classes used : login-background, login-card, header-icon,
 *                    header-title, header-subtitle, input-label,
 *                    text-input, password-input, login-button,
 *                    error-message
 * ══════════════════════════════════════════════════════════════════════
 */
public class LoginView {

    // ── ENTRY POINT ───────────────────────────────────────────────────
    /** Builds and returns the complete login Scene. */
    public static Scene getScene() {

        /* ── OUTER BACKGROUND ──────────────────────────────────────── */
        // Full-screen VBox that holds the gradient background.
        // The gradient itself is painted by the CSS class "login-background".
        VBox background = new VBox();
        background.getStyleClass().add("login-background");

        background.setFillWidth(true);
        background.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        /* ── GLASS CARD ────────────────────────────────────────────── */
        // The floating card in the centre of the screen.
        // "login-card" applies frosted-glass effect via CSS.
        VBox card = new VBox(22);
        card.getStyleClass().add("login-card");
        card.setPrefWidth(430);
        card.setMaxWidth(700);
        card.setAlignment(Pos.TOP_CENTER);

        /* ── HEADER SECTION ─────────────────────────────────────────── */
        VBox header = createHeader();

        /* ── EMAIL FIELD ────────────────────────────────────────────── */
        // createInputSection returns a labelled VBox wrapping an input.
        VBox emailSection   = createInputSection("Email Address", "name@university.edu.pk");
        TextField emailField = (TextField) emailSection.lookup("TextField");

        /* ── PASSWORD FIELD ─────────────────────────────────────────── */
        VBox passwordSection    = createInputSection("Password", "Enter your password");
        PasswordField passwordField =
            (PasswordField) passwordSection.lookup("PasswordField");

        /* ── ERROR LABEL ────────────────────────────────────────────── */
        // Initially hidden; shown with a slide animation on validation failure.
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-message");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);   // takes no space when hidden
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(330);

        /* ── SIGN IN BUTTON ─────────────────────────────────────────── */
        Button loginButton = new Button("Sign In");
        loginButton.getStyleClass().add("login-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        // Wire up the login logic
        loginButton.setOnAction(e ->
            handleLogin(emailField, passwordField, errorLabel));

        // Also allow pressing Enter in password field to submit
        passwordField.setOnAction(e ->
            handleLogin(emailField, passwordField, errorLabel));

        /* ── ASSEMBLE CARD ──────────────────────────────────────────── */
        card.getChildren().addAll(
            header,
            emailSection,
            passwordSection,
            errorLabel,
            loginButton
        );

        /* ── CENTRE CARD ON BACKGROUND ──────────────────────────────── */
        StackPane centerContainer = new StackPane(card);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.setPadding(new Insets(40));
        centerContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        background.getChildren().add(centerContainer);
        VBox.setVgrow(centerContainer, javafx.scene.layout.Priority.ALWAYS);

        /* ── ENTRANCE ANIMATION ─────────────────────────────────────── */
        animateCardIn(card);

        /* ── BUILD SCENE ────────────────────────────────────────────── */
        Scene scene = new Scene(background, 1100, 700);
        // Load the shared stylesheet
        scene.getStylesheets().add(
            LoginView.class.getResource("/styles.css").toExternalForm());

        return scene;
    }


    // ── PRIVATE HELPERS ───────────────────────────────────────────────

    /**
     * Creates the logo + title + subtitle header block.
     * Each element gets its own CSS class for independent styling.
     */
    private static VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        // Extra bottom margin separates header from inputs
        header.setPadding(new Insets(0, 0, 10, 0));

        // ── Clock emoji icon — glows via CSS drop-shadow ──
        Label icon = new Label("⏰");
        icon.getStyleClass().add("header-icon");

        // Create the color adjustment to turn it white
        ColorAdjust whiteOut = new ColorAdjust();
        whiteOut.setSaturation(-1.0); // Remove color
        whiteOut.setBrightness(1.0);   // Turn to white
        icon.setEffect(whiteOut);
        // ── App name ──
        Label title = new Label("SlotSync");
        title.getStyleClass().add("header-title");

        // ── Tagline ──
        Label subtitle = new Label("Effortless Appointment Scheduling");
        subtitle.getStyleClass().add("header-subtitle");

        header.getChildren().addAll(icon, title, subtitle);
        return header;
    }

    /**
     * Creates a labelled input section (label above, field below).
     *
     * @param labelText   The field label (e.g. "Email Address")
     * @param placeholder The grey placeholder inside the field
     * @return            A VBox containing the label + the input control
     */
    private static VBox createInputSection(String labelText, String placeholder) {
        VBox section = new VBox(8);

        // ── Label ──
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");

        // ── Input control — password or plain text ──
        TextInputControl input;
        if (labelText.contains("Password")) {
            // PasswordField masks characters
            input = new PasswordField();
            input.getStyleClass().add("password-input");
        } else {
            input = new TextField();
            input.getStyleClass().add("text-input");
        }

        input.setPromptText(placeholder);
        // Height is also enforced in CSS; this is a Java-side fallback
        input.setPrefHeight(48);

        section.getChildren().addAll(label, input);
        return section;
    }

    /**
     * Handles the login button action:
     *  1. Validates inputs (empty check, email format)
     *  2. Calls AuthService.login()
     *  3. Routes to the correct dashboard on success
     *  4. Shows an animated error label on failure
     */
    private static void handleLogin(
            TextField emailField,
            PasswordField passwordField,
            Label errorLabel) {

        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // ── Client-side validation ──
        if (email.isEmpty() || password.isEmpty()) {
            showError(errorLabel, "⚠  Please enter both email and password.");
            return;
        }

        if (!email.contains("@")) {
            showError(errorLabel, "⚠  Please enter a valid email address.");
            return;
        }

        // ── Service call ──
        try {
            AuthService authService = new AuthService();
            User user = authService.login(email, password);

            if (user == null) {
                showError(errorLabel, "⚠  Invalid email or password. Please try again.");
                return;
            }

            // Hide any previous error
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            // Store current user and navigate
            Main.setCurrentUser(user);

            if (user instanceof Student) {
                Main.switchScene(StudentView.getScene((Student) user));
            } else if (user instanceof Professor) {
                Main.switchScene(ProfessorView.getScene((Professor) user));
            }

        } catch (Exception ex) {
            showError(errorLabel, "⚠  Connection error. Please try again.");
            ex.printStackTrace();
        }
    }

    /**
     * Makes the error label visible and plays a slide-down + fade animation.
     * Uses setManaged(true) so the layout reflows to include the label height.
     */
    private static void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Slide down from slightly above
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), errorLabel);
        slide.setFromY(-6);
        slide.setToY(0);

        // Simultaneously fade in
        FadeTransition fade = new FadeTransition(Duration.millis(250), errorLabel);
        fade.setFromValue(0);
        fade.setToValue(1);

        // Run both together
        ParallelTransition anim = new ParallelTransition(slide, fade);
        anim.play();
    }

    /**
     * Card entrance animation: the card fades in while scaling up
     * from 94 % → 100 %, giving a "pop into place" feel.
     */
    private static void animateCardIn(VBox card) {
        // Start invisible and slightly small
        card.setOpacity(0);
        card.setScaleX(0.94);
        card.setScaleY(0.94);

        // Fade 0 → 1
        FadeTransition fade = new FadeTransition(Duration.millis(550), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        // Scale 0.94 → 1.0
        ScaleTransition scale = new ScaleTransition(Duration.millis(550), card);
        scale.setFromX(0.94);
        scale.setFromY(0.94);
        scale.setToX(1.0);
        scale.setToY(1.0);

        // Play both simultaneously
        ParallelTransition entrance = new ParallelTransition(fade, scale);
        entrance.play();
    }
}
