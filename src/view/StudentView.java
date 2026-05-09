package view;

import java.util.List;

import enums.AppointmentReason;
import enums.AppointmentStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import main.Main;
import model.Appointment;
import model.Student;
import model.WaitlistEntry;
import service.AppointmentService;
import service.WaitlistService;

/**
 * ══════════════════════════════════════════════════════════════════════
 * StudentView — Dashboard for student accounts
 *
 * Design language: "Obsidian Glass" — identical dark-navy palette to
 * LoginView and ProfessorView for full cross-screen consistency.
 *
 * Sub-views (swapped in contentArea StackPane):
 *   • My Appointments    — paginated table of all student appointments
 *   • Book Appointment   — search + filter slots, one-click booking
 *   • My Waitlist        — all waitlist entries for the student
 *   • Cancel Appointment — filtered list of cancellable appointments
 *
 * Style approach:
 *   • Java-side inline styles for node backgrounds/layouts that the CSS
 *     class selectors don't reliably reach (HBox, VBox, StackPane).
 *   • CSS classes (from styles.css) for TableView, ComboBox, scrollbars,
 *     dialogs, and the table cells.
 *   • All colours reference the constants at the top of this file so
 *     the palette stays in sync with styles.css.
 * ══════════════════════════════════════════════════════════════════════
 */
public class StudentView {

    // ── SINGLETON SERVICE ─────────────────────────────────────────────
    // Shared service instance avoids re-creating it on every sub-view build
    private static final AppointmentService appointmentService =
        new AppointmentService();

    // ── COLOUR PALETTE (mirrors styles.css variables) ─────────────────
    private static final String BG_DARKEST   = "#0F1525";               // root bg
    private static final String BG_SIDEBAR   = "#1B264F";               // sidebar bg
    private static final String BG_CARD      = "rgba(255,255,255,0.025)"; // glass card
    private static final String INDIGO       = "#274690";               // primary accent
    private static final String INDIGO_LIGHT = "#576CA8";               // lighter accent
    private static final String SUCCESS      = "#34D399";               // approved / success
    private static final String DANGER       = "#F87171";               // rejected / danger
    private static final String AMBER        = "#FBBF24";               // pending
    private static final String BLUE         = "#60A5FA";               // waitlisted
    private static final String TEAL         = "#2DD4BF";               // available
    private static final String TEXT_PRIMARY = "#E2E8F0";               // primary text
    private static final String TEXT_MUTED   = "#64748B";               // secondary text
    private static final String BORDER       = "rgba(255,255,255,0.07)"; // glass border


    // ══════════════════════════════════════════════════════════════════
    // SCENE ENTRY POINT
    // ══════════════════════════════════════════════════════════════════

    /**
     * Builds the full student dashboard Scene.
     *
     * Layout:
     *   BorderPane
     *     top    → topBar (gradient HBox)
     *     left   → sidebar (dark-navy VBox)
     *     center → contentArea (StackPane — sub-views swap here)
     */
    public static Scene getScene(Student student) {

        // ── TOP BAR ───────────────────────────────────────────────────
        // App logo on the left, welcome chip on the right
        Label appTitle = new Label("⏰  SlotSync");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        appTitle.setTextFill(Color.WHITE);
        appTitle.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(12, 22, 44, 0.3), 8, 0, 0, 0);"
        );

        // Pill-shaped welcome badge for the student name
        Label welcomeLabel = new Label(
            student.getFirstName() + " " + student.getLastName());
        welcomeLabel.setFont(Font.font("Segoe UI", 13));
        welcomeLabel.setTextFill(Color.web(TEXT_PRIMARY));
        welcomeLabel.setStyle(
            "-fx-background-color: rgba(39,70,144,0.15);" +
            "-fx-border-color: rgba(39,70,144,0.25);" +
            "-fx-border-radius: 20;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 5 14;"
        );

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, appTitle, topSpacer, welcomeLabel);
        topBar.setPadding(new Insets(14, 28, 14, 28));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");

        // ── SIDEBAR ───────────────────────────────────────────────────
        VBox sidebar = createStyledSidebar(student);

        // ── CONTENT AREA ──────────────────────────────────────────────
        // Defaults to My Appointments on load
        BorderPane contentArea = new BorderPane();
        contentArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentArea.setPadding(new Insets(28));
        contentArea.setCenter(buildViewAppointments(student));
        contentArea.getStyleClass().add("content-area");


        // ── WIRE UP SIDEBAR BUTTONS ───────────────────────────────────
        // Look up buttons by their JavaFX ID set inside createStyledSidebar()
        Button viewApptBtn   = (Button) sidebar.lookup("#viewApptBtn");
        Button bookApptBtn   = (Button) sidebar.lookup("#bookApptBtn");
        Button waitlistBtn   = (Button) sidebar.lookup("#waitlistBtn");
        Button cancelApptBtn = (Button) sidebar.lookup("#cancelApptBtn");
        Button logoutBtn     = (Button) sidebar.lookup("#logoutBtn");

        viewApptBtn.setOnAction(e -> {
            setActive(viewApptBtn, bookApptBtn, waitlistBtn, cancelApptBtn);
            contentArea.setCenter(buildViewAppointments(student));
        });

        bookApptBtn.setOnAction(e -> {
            setActive(bookApptBtn, viewApptBtn, waitlistBtn, cancelApptBtn);
            contentArea.setCenter(buildBookAppointment(student));
        });

        waitlistBtn.setOnAction(e -> {
            setActive(waitlistBtn, viewApptBtn, bookApptBtn, cancelApptBtn);
            contentArea.setCenter(buildViewWaitlist(student));
        });

        cancelApptBtn.setOnAction(e -> {
            setActive(cancelApptBtn, viewApptBtn, bookApptBtn, waitlistBtn);
            contentArea.setCenter(buildCancelAppointment(student));
        });

        logoutBtn.setOnAction(e -> Main.showLoginScreen());

        // ── ROOT ──────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
            StudentView.class.getResource("/styles.css").toExternalForm());
        return scene;
    }


    // ══════════════════════════════════════════════════════════════════
    // SIDEBAR FACTORY
    // ══════════════════════════════════════════════════════════════════

    /**
     * Creates the left navigation sidebar.
     * Buttons are given JavaFX IDs so getScene() can look them up.
     */
    private static VBox createStyledSidebar(Student student) {

        Button viewApptBtn   = createSidebarButton("📋  My Appointments");
        Button bookApptBtn   = createSidebarButton("➕  Book Appointment");
        Button waitlistBtn   = createSidebarButton("⏳  My Waitlist");
        Button cancelApptBtn = createSidebarButton("❌  Cancel Appointment");
        Button logoutBtn     = createLogoutButton("🚪  Logout");

        // IDs used for lookup() in getScene()
        viewApptBtn.setId("viewApptBtn");
        bookApptBtn.setId("bookApptBtn");
        waitlistBtn.setId("waitlistBtn");
        cancelApptBtn.setId("cancelApptBtn");
        logoutBtn.setId("logoutBtn");

        // "My Appointments" is active by default on load
        viewApptBtn.setStyle(buildActiveSidebarStyle());

        // Spacer pushes logout to the bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Small uppercase section label
        Label navLabel = new Label("NAVIGATION");
        navLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        navLabel.setTextFill(Color.web("#475569"));
        navLabel.setPadding(new Insets(14, 0, 4, 16));

        VBox sidebar = new VBox(6,
            navLabel,
            viewApptBtn, bookApptBtn, waitlistBtn, cancelApptBtn,
            spacer,
            logoutBtn
        );
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setMinWidth(210);
        sidebar.setPrefWidth(230);  
        sidebar.setMaxWidth(230);
        sidebar.setStyle(
            "-fx-background-color: " + BG_SIDEBAR + ";" +
            "-fx-border-color: rgba(39,70,144,0.20);" +
            "-fx-border-width: 0 1 0 0;"
        );

        return sidebar;
    }


    // ══════════════════════════════════════════════════════════════════
    // VIEW: MY APPOINTMENTS
    // ══════════════════════════════════════════════════════════════════

    /**
     * Full appointment history for the student.
     * Columns: ID, SlotID, Professor, Date, Time, Reason, Status, Booked At.
     * Status column is colour-coded.
     */
    private static VBox buildViewAppointments(Student student) {

        // ── Appointment ID ──
        TableColumn<Appointment, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        idCol.setPrefWidth(55);

        // ── Slot ID ──
        TableColumn<Appointment, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotId"));
        slotCol.setPrefWidth(55);

        // ── Professor name (fetched from appointment model) ──
        TableColumn<Appointment, String> profCol = new TableColumn<>("Professor");
        profCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getProfessorName()));
        profCol.setPrefWidth(130);

        // ── Slot date ──
        TableColumn<Appointment, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSlotDate() != null
                    ? cellData.getValue().getSlotDate().toString() : "—"));
        dateCol.setPrefWidth(100);

        // ── Time range ──
        TableColumn<Appointment, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> {
            Appointment a = cellData.getValue();
            String t = (a.getSlotStartTime() != null && a.getSlotEndTime() != null)
                ? a.getSlotStartTime() + " – " + a.getSlotEndTime() : "—";
            return new javafx.beans.property.SimpleStringProperty(t);
        });
        timeCol.setPrefWidth(120);

        // ── Reason ──
        TableColumn<Appointment, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonCol.setPrefWidth(110);

        // ── Status — colour-coded ──
        TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> buildApptStatusCell());
        statusCol.setPrefWidth(100);

        // ── Booked At timestamp ──
        TableColumn<Appointment, String> bookedCol = new TableColumn<>("Booked At");
        bookedCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt().toString()));
        bookedCol.setPrefWidth(155);

        // ── Build table ──
        TableView<Appointment> table = buildStyledTable();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        table.getColumns().addAll(
            idCol, slotCol, profCol, dateCol, timeCol, reasonCol, statusCol, bookedCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        List<Appointment> appointments =
            appointmentService.getStudentAppointments(student.getUserId());
        table.getItems().addAll(appointments);

        // ── Count badge ──
        Label countLabel = new Label(
            appointments.size() + " appointment" + (appointments.size() == 1 ? "" : "s"));
        countLabel.setFont(Font.font("Segoe UI", 13));
        countLabel.setTextFill(Color.web(TEXT_MUTED));

        return wrapInCard("📋  My Appointments", table, countLabel);
    }


    // ══════════════════════════════════════════════════════════════════
    // VIEW: BOOK APPOINTMENT
    // ══════════════════════════════════════════════════════════════════

    /**
     * Allows a student to:
     *  1. Select a reason from the AppointmentReason enum
     *  2. Filter professors by name using the search field
     *  3. Click "Book" in the table row to confirm the booking
     */
    private static VBox buildBookAppointment(Student student) {

        // ── Reason selector ──
        Label reasonLabel = styledFormLabel("Reason:");
        ComboBox<AppointmentReason> reasonBox = new ComboBox<>();
        reasonBox.getItems().addAll(AppointmentReason.values());
        reasonBox.setPromptText("Choose a reason…");
        reasonBox.setPrefWidth(260);
        reasonBox.setPrefHeight(40);
        styleComboBox(reasonBox);

        // ── Professor search field ──
        Label searchLabel = styledFormLabel("Search Professor:");
        TextField searchField = new TextField();
        searchField.setPromptText("Type professor name…");
        searchField.setPrefWidth(260);
        searchField.setPrefHeight(40);
        styleTextField(searchField);

        // ── Input row (reason + search side-by-side) ──
        HBox inputRow = new HBox(24, reasonLabel, reasonBox, searchLabel, searchField);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPadding(new Insets(14, 16, 14, 16));
        inputRow.setStyle(
            "-fx-background-color: rgba(39,70,144,0.07);" +
            "-fx-border-color: rgba(39,70,144,0.14);" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;"
        );

        // ── Feedback label (success / error messages) ──
        Label feedbackLabel = new Label("");
        feedbackLabel.setFont(Font.font("Segoe UI", 13));
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        feedbackLabel.setWrapText(true);

        // ── Slots table ──
        // Columns: Professor, Date, Start, End, Spots Left, [Book button]
        TableColumn<Object[], String> profCol = new TableColumn<>("Professor");
        profCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                (String) data.getValue()[2]));
        profCol.setPrefWidth(140);

        TableColumn<Object[], String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[3].toString()));
        dateCol.setPrefWidth(110);

        TableColumn<Object[], String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[4].toString()));
        startCol.setPrefWidth(90);

        TableColumn<Object[], String> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[5].toString()));
        endCol.setPrefWidth(90);

        // Spots Left — colour-coded (0 = red, >0 = teal)
        TableColumn<Object[], String> spotsCol = new TableColumn<>("Spots");
        spotsCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[6].toString()));
        spotsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    int spots = Integer.parseInt(item);
                    String colour = spots > 0 ? TEAL : DANGER;
                    setStyle("-fx-text-fill: " + colour + "; -fx-font-weight: bold;");
                }
            }
        });
        spotsCol.setPrefWidth(65);

        // ── Book button column ──
        TableColumn<Object[], Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(90);
        actionCol.setCellFactory(col -> new TableCell<>() {
            // Button created once per cell for efficiency
            private final Button bookBtn = createPrimaryButton("Book");

            {
                bookBtn.setOnAction(e -> {
                    Object[] row = getTableView().getItems().get(getIndex());
                    int slotId = (int) row[0];

                    AppointmentReason reason = reasonBox.getValue();
                    if (reason == null) {
                        showFeedback(feedbackLabel,
                            "⚠  Please select a reason first.", false);
                        return;
                    }

                    boolean success = appointmentService.bookAppointment(
                        student.getUserId(), slotId, reason);

                    if (success) {
                        showFeedback(feedbackLabel,
                            "✓  Appointment booked successfully!", true);
                        // Refresh table to update spots count
                        loadAndFilter(getTableView(), searchField.getText().trim());
                    } else {
                        showFeedback(feedbackLabel,
                            "✗  Booking failed. Slot may be full.", false);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : bookBtn);
            }
        });

        TableView<Object[]> table = buildStyledTable();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        table.getColumns().addAll(profCol, dateCol, startCol, endCol, spotsCol, actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Initial load — show all available slots
        loadAndFilter(table, "");

        // Live filter as user types professor name
        searchField.textProperty().addListener((obs, old, newVal) ->
            loadAndFilter(table, newVal.trim()));

        return wrapInCard("➕  Book an Appointment",
            table, inputRow, feedbackLabel);
    }


    // ══════════════════════════════════════════════════════════════════
    // VIEW: MY WAITLIST
    // ══════════════════════════════════════════════════════════════════

    /**
     * Shows all waitlist entries for this student.
     * Columns: Waitlist ID, Slot ID, Student name, Priority, Joined At.
     */
    private static VBox buildViewWaitlist(Student student) {

        TableColumn<WaitlistEntry, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("waitlistId"));
        idCol.setPrefWidth(60);

        TableColumn<WaitlistEntry, Integer> slotCol = new TableColumn<>("Slot ID");
        slotCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getSlot().getSlotID()).asObject());
        slotCol.setPrefWidth(80);

        TableColumn<WaitlistEntry, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStudent().getFirstName() + " " +
                cellData.getValue().getStudent().getLastName()));
        studentCol.setPrefWidth(160);

        // Priority score — highlighted in indigo
        TableColumn<WaitlistEntry, Integer> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priorityScore"));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill: " + INDIGO_LIGHT +
                             "; -fx-font-weight: bold;");
                }
            }
        });
        priorityCol.setPrefWidth(90);

        TableColumn<WaitlistEntry, String> joinedCol = new TableColumn<>("Joined At");
        joinedCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getJoinedAt().toString()));
        joinedCol.setPrefWidth(160);

        TableView<WaitlistEntry> table = buildStyledTable();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        table.getColumns().addAll(idCol, slotCol, studentCol, priorityCol, joinedCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        WaitlistService waitlistService = new WaitlistService();
        List<WaitlistEntry> waitlist =
            waitlistService.getWaitlistByStudent(student.getUserId());
        table.getItems().addAll(waitlist);

        Label countLabel = new Label(
            waitlist.size() + " waitlist entr" + (waitlist.size() == 1 ? "y" : "ies"));
        countLabel.setFont(Font.font("Segoe UI", 13));
        countLabel.setTextFill(Color.web(TEXT_MUTED));

        return wrapInCard("⏳  My Waitlist", table, countLabel);
    }


    // ══════════════════════════════════════════════════════════════════
    // VIEW: CANCEL APPOINTMENT
    // ══════════════════════════════════════════════════════════════════

    /**
     * Shows only cancellable appointments (PENDING, APPROVED, WAITLISTED).
     * Each row has a "Cancel" danger button.
     */
    private static VBox buildCancelAppointment(Student student) {

        // Info banner
        Label infoBanner = new Label(
            "ℹ  Only PENDING, APPROVED, and WAITLISTED appointments can be cancelled.");
        infoBanner.setFont(Font.font("Segoe UI", 13));
        infoBanner.setTextFill(Color.web(AMBER));
        infoBanner.setWrapText(true);
        infoBanner.setPadding(new Insets(10, 14, 10, 14));
        infoBanner.setStyle(
            "-fx-background-color: rgba(251,191,36,0.08);" +
            "-fx-border-color: rgba(251,191,36,0.20);" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;"
        );

        // ── Columns ──
        TableColumn<Appointment, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        idCol.setPrefWidth(60);

        TableColumn<Appointment, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotId"));
        slotCol.setPrefWidth(60);

        TableColumn<Appointment, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonCol.setPrefWidth(130);

        // Status — colour-coded
        TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> buildApptStatusCell());
        statusCol.setPrefWidth(100);

        // ── Cancel button column ──
        TableColumn<Appointment, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(95);
        actionCol.setCellFactory(col -> new TableCell<>() {
            // Danger button created once per cell
            private final Button cancelBtn = createDangerButton("Cancel");

            {
                cancelBtn.setOnAction(e -> {
                    Appointment appt = getTableView().getItems().get(getIndex());

                    boolean success = appointmentService.cancelAppointment(
                        appt.getAppointmentId(), appt.getSlotId());

                    if (success) {
                        // Remove the row immediately so the table updates
                        getTableView().getItems().remove(appt);
                        showStyledAlert(Alert.AlertType.INFORMATION,
                            "Cancelled",
                            "Appointment Cancelled",
                            "Your appointment has been cancelled successfully.");
                    } else {
                        showStyledAlert(Alert.AlertType.ERROR,
                            "Failed",
                            "Cancellation Failed",
                            "Could not cancel the appointment. Please try again.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : cancelBtn);
            }
        });

        TableView<Appointment> table = buildStyledTable();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        table.getColumns().addAll(idCol, slotCol, reasonCol, statusCol, actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Filter: only include cancellable statuses
        List<Appointment> all =
            appointmentService.getStudentAppointments(student.getUserId());
        for (Appointment a : all) {
            if (a.getStatus() == AppointmentStatus.PENDING
                || a.getStatus() == AppointmentStatus.APPROVED
                || a.getStatus() == AppointmentStatus.WAITLISTED) {
                table.getItems().add(a);
            }
        }

        // ── Empty state ──
        if (table.getItems().isEmpty()) {
            Label emptyLabel = new Label("No cancellable appointments found.");
            emptyLabel.setFont(Font.font("Segoe UI", 14));
            emptyLabel.setTextFill(Color.web(TEXT_MUTED));

            VBox card = new VBox(16);
            card.getChildren().addAll(
                buildCardHeader("❌  Cancel an Appointment", ""),
                infoBanner,
                emptyLabel
            );
            card.setPadding(new Insets(24));
            applyCardStyle(card);
            return card;
        }

        return wrapInCard("❌  Cancel an Appointment",
            table, infoBanner, null);
    }


    // ══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS — STYLE & LAYOUT
    // ══════════════════════════════════════════════════════════════════

    // ── Sidebar nav button (inactive state) ──────────────────────────
    private static Button createSidebarButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(196);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", 13));
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(buildInactiveStyle());

        btn.setOnMouseEntered(e -> {
            // Don't override active style
            if (!btn.getStyle().contains("rgba(39,70,144,0.30)")) {
                btn.setStyle(buildHoverStyle());
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("rgba(39,70,144,0.30)")) {
                btn.setStyle(buildInactiveStyle());
            }
        });
        return btn;
    }

    /** Sets one button as active, all others as inactive. */
    private static void setActive(Button active, Button... others) {
        active.setStyle(buildActiveSidebarStyle());
        for (Button b : others) {
            b.setStyle(buildInactiveStyle());
            b.setOnMouseEntered(e -> {
                if (!b.getStyle().contains("rgba(39,70,144,0.30)")) {
                    b.setStyle(buildHoverStyle());
                }
            });
            b.setOnMouseExited(e -> {
                if (!b.getStyle().contains("rgba(39,70,144,0.30)")) {
                    b.setStyle(buildInactiveStyle());
                }
            });
        }
    }

    private static String buildInactiveStyle() {
        return "-fx-background-color: transparent;" +
               "-fx-text-fill: #94A3B8;" +
               "-fx-font-size: 13;" +
               "-fx-font-weight: 600;" +
               "-fx-padding: 10 16;" +
               "-fx-background-radius: 8;" +
               "-fx-cursor: hand;" +
               "-fx-alignment: CENTER_LEFT;";
    }

    private static String buildHoverStyle() {
        return "-fx-background-color: rgba(39,70,144,0.20);" +
               "-fx-text-fill: #8FA8D0;" +
               "-fx-font-size: 13;" +
               "-fx-font-weight: 600;" +
               "-fx-padding: 10 16;" +
               "-fx-background-radius: 8;" +
               "-fx-cursor: hand;" +
               "-fx-alignment: CENTER_LEFT;";
    }

    private static String buildActiveSidebarStyle() {
        return "-fx-background-color: rgba(39,70,144,0.30);" +
               "-fx-text-fill: " + INDIGO_LIGHT + ";" +
               "-fx-font-size: 13;" +
               "-fx-font-weight: bold;" +
               "-fx-padding: 10 16 10 13;" +
               "-fx-background-radius: 8;" +
               "-fx-cursor: hand;" +
               "-fx-alignment: CENTER_LEFT;" +
               "-fx-border-color: " + INDIGO + " transparent transparent transparent;" +
               "-fx-border-width: 0 0 0 3;" +
               "-fx-border-radius: 0;";
    }

    // ── Logout button (danger red) ────────────────────────────────────
    private static Button createLogoutButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(196);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setAlignment(Pos.CENTER_LEFT);
        applyLogoutStyle(btn, false);
        btn.setOnMouseEntered(e -> applyLogoutStyle(btn, true));
        btn.setOnMouseExited(e  -> applyLogoutStyle(btn, false));
        return btn;
    }

    private static void applyLogoutStyle(Button btn, boolean hovered) {
        String bg = hovered ? "rgba(239,68,68,0.22)" : "rgba(239,68,68,0.12)";
        String fg = hovered ? "#FECACA" : "#FCA5A5";
        String border = hovered ? "rgba(239,68,68,0.35)" : "rgba(239,68,68,0.22)";
        String glow = hovered
            ? "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.20), 6, 0, 0, 0);"
            : "";
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-padding: 10 16;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: CENTER_LEFT;" +
            glow
        );
    }

    // ── Generic styled TableView (dark glass) ─────────────────────────
    /** Returns a new TableView with dark-background inline style. */
    private static <T> TableView<T> buildStyledTable() {
        TableView<T> table = new TableView<>();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // Background set transparent so .table-view in CSS can control it
        table.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: rgba(255,255,255,0.07);" +
            "-fx-border-radius: 10;" +
            "-fx-padding: 0;"
        );
        return table;
    }

    // ── Colour-coded status cells ─────────────────────────────────────
    /** TableCell that colours appointment statuses. */
    private static TableCell<Appointment, String> buildApptStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                             apptStatusColour(item) + ";");
                }
            }
        };
    }

    private static String apptStatusColour(String status) {
        return switch (status) {
            case "APPROVED"   -> SUCCESS;
            case "REJECTED"   -> DANGER;
            case "PENDING"    -> AMBER;
            case "WAITLISTED" -> BLUE;
            default           -> TEXT_PRIMARY;
        };
    }

    // ── Card container helpers ────────────────────────────────────────

    /**
     * Wraps a title label + table into a glass card VBox.
     * countLabel is placed below the title.
     */
    private static VBox wrapInCard(String title, TableView<?> table, Label countLabel) {
        VBox card = new VBox(14);
        card.getChildren().addAll(buildCardHeader(title, countLabel), table);
        card.setPadding(new Insets(24));
        applyCardStyle(card);
        VBox.setVgrow(table, Priority.ALWAYS);
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    /**
     * Wraps title + extra node + table into a card.
     * Used by Book and Cancel views which need an input row above the table.
     */
    private static VBox wrapInCard(String title, TableView<?> table,
                                    javafx.scene.Node extraNode,
                                    Label feedbackLabel) {
        VBox card = new VBox(14);
        card.setFillWidth(true);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web(TEXT_PRIMARY));

        card.getChildren().add(titleLabel);
        if (extraNode != null)    card.getChildren().add(extraNode);
        if (feedbackLabel != null) card.getChildren().add(feedbackLabel);
        card.getChildren().add(table);

        card.setPadding(new Insets(24));
        applyCardStyle(card);
        VBox.setVgrow(table, Priority.ALWAYS);
        return card;
    }

    /** Builds a header block: title label + count label stacked vertically. */
    private static VBox buildCardHeader(String title, Label countLabel) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web(TEXT_PRIMARY));

        VBox header = new VBox(4, titleLabel);
        if (countLabel != null) header.getChildren().add(countLabel);
        return header;
    }

    /** Builds a plain title + subtitle-string header. */
    private static VBox buildCardHeader(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web(TEXT_PRIMARY));

        VBox header = new VBox(4, titleLabel);
        if (subtitle != null && !subtitle.isEmpty()) {
            Label sub = new Label(subtitle);
            sub.setFont(Font.font("Segoe UI", 13));
            sub.setTextFill(Color.web(TEXT_MUTED));
            header.getChildren().add(sub);
        }
        return header;
    }

    /** Applies the frosted-glass card style to any VBox. */
    private static void applyCardStyle(VBox card) {
        card.setFillWidth(true);
        card.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 20, 0, 0, 6);"
        );
    }

    // ── Button factories ──────────────────────────────────────────────

    /** Indigo gradient "primary" action button (e.g. Book). */
    private static Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        applyPrimaryBtnStyle(btn, false);
        btn.setOnMouseEntered(e -> applyPrimaryBtnStyle(btn, true));
        btn.setOnMouseExited(e  -> applyPrimaryBtnStyle(btn, false));
        return btn;
    }

    private static void applyPrimaryBtnStyle(Button btn, boolean hovered) {
        String bg = hovered
            ? "linear-gradient(to bottom,#576CA8,#274690)"
            : "linear-gradient(to bottom," + INDIGO + ",#1B264F)";
        String glow = hovered
            ? "dropshadow(gaussian, rgba(39,70,144,0.40), 8, 0, 0, 3)"
            : "dropshadow(gaussian, rgba(39,70,144,0.25), 6, 0, 0, 2)";
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 7 18;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-effect: " + glow + ";"
        );
    }

    /** Red gradient "danger" action button (e.g. Cancel). */
    private static Button createDangerButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        applyDangerBtnStyle(btn, false);
        btn.setOnMouseEntered(e -> applyDangerBtnStyle(btn, true));
        btn.setOnMouseExited(e  -> applyDangerBtnStyle(btn, false));
        return btn;
    }

    private static void applyDangerBtnStyle(Button btn, boolean hovered) {
        String bg = hovered
            ? "linear-gradient(to bottom,#F87171,#EF4444)"
            : "linear-gradient(to bottom,#EF4444,#DC2626)";
        String glow = hovered
            ? "dropshadow(gaussian, rgba(239,68,68,0.60), 8, 0, 0, 3)"
            : "dropshadow(gaussian, rgba(239,68,68,0.40), 6, 0, 0, 2)";
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 7 18;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-effect: " + glow + ";"
        );
    }

    // ── Form control styles ───────────────────────────────────────────

    /** Bold indigo-tinted label for form field headings. */
    private static Label styledFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(INDIGO_LIGHT));
        return lbl;
    }

    private static final String TEXTFIELD_NORMAL =
    "-fx-background-color: rgba(255,255,255,0.05);" +
    "-fx-border-color: rgba(87,108,168,0.25);" +
    "-fx-border-width: 1.5;" +
    "-fx-border-radius: 8;" +
    "-fx-background-radius: 8;" +
    "-fx-text-fill: #E2E8F0;" +
    "-fx-prompt-text-fill: #475569;" +
    "-fx-font-size: 13;" +
    "-fx-padding: 10 14;";

private static final String TEXTFIELD_FOCUSED =
    "-fx-background-color: rgba(39,70,144,0.07);" +
    "-fx-border-color: #274690;" +
    "-fx-border-width: 2;" +
    "-fx-border-radius: 8;" +
    "-fx-background-radius: 8;" +
    "-fx-text-fill: #E2E8F0;" +
    "-fx-prompt-text-fill: #475569;" +
    "-fx-font-size: 13;" +
    "-fx-padding: 10 14;" +
    "-fx-effect: dropshadow(gaussian, rgba(39,70,144,0.15), 8, 0, 0, 0);";

    /** Dark-glass text field style. */
    private static void styleTextField(TextField tf) {

    tf.setStyle(TEXTFIELD_NORMAL);

    tf.focusedProperty().addListener((obs, wasF, isF) -> {
        tf.setStyle(isF ? TEXTFIELD_FOCUSED : TEXTFIELD_NORMAL);
    });
}

    /** Dark-glass ComboBox style. */
    private static <T> void styleComboBox(ComboBox<T> combo) {
        combo.setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-border-color: rgba(87,108,168,0.25);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-text-fill: #CBD5E1;" +
            "-fx-font-size: 13;"
        );
    }

    // ── Feedback label helper ─────────────────────────────────────────
    /**
     * Shows the feedback label with appropriate colour & style.
     * @param success true = green success style, false = red error style
     */
    private static void showFeedback(Label lbl, String message, boolean success) {
        lbl.setText(message);
        lbl.setVisible(true);
        lbl.setManaged(true);
        if (success) {
            lbl.setTextFill(Color.web(SUCCESS));
            lbl.setStyle(
                "-fx-background-color: rgba(52,211,153,0.10);" +
                "-fx-border-color: rgba(52,211,153,0.25);" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 10 14;"
            );
        } else {
            lbl.setTextFill(Color.web(DANGER));
            lbl.setStyle(
                "-fx-background-color: rgba(248,113,113,0.10);" +
                "-fx-border-color: rgba(248,113,113,0.25);" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 10 14;"
            );
        }
    }

    // ── Load + filter available slots ─────────────────────────────────
    /**
     * Loads all available slots from the service and filters by professor name.
     * Called on load and whenever the search field text changes.
     */
    private static void loadAndFilter(TableView<Object[]> table, String search) {
        List<Object[]> allSlots = appointmentService.getAvailableSlots();
        table.getItems().clear();
        for (Object[] row : allSlots) {
            String profName = (String) row[2];
            if (search.isEmpty() ||
                profName.toLowerCase().contains(search.toLowerCase())) {
                table.getItems().add(row);
            }
        }
    }

    // ── Styled Alert dialog ───────────────────────────────────────────
    /**
     * Shows an Alert and applies the CSS stylesheet so it inherits
     * the dark-glass dialog-pane style defined in styles.css.
     */
    private static void showStyledAlert(Alert.AlertType type,
                                        String title,
                                        String header,
                                        String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        // Apply the stylesheet to the alert dialog
        alert.getDialogPane().getStylesheets().add(
            StudentView.class.getResource("/styles.css").toExternalForm());
        alert.showAndWait();
    }
}
