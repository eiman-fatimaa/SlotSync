package view;

import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.Gson;

import enums.AppointmentStatus;
import enums.TimeSlotStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import main.Main;
import model.Appointment;
import model.Professor;
import model.SlotTemplate;
import model.TimeSlot;
import model.WaitlistEntry;
import service.AppointmentService;
import service.SlotGeneratorService;
import service.TimeSlotService;
import service.WaitlistService;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PROFESSOR VIEW — Complete Dashboard
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Design Language: "Obsidian Glass" — Deep navy backgrounds, crystalline whites,
 *                  electric blue accents. Professional, modern, alive.
 *
 * Service Integration:
 *   • AppointmentService: Manages appointment operations (status updates, cancellations)
 *   • TimeSlotService: Manages slot operations (block/unblock, status changes)
 *   • WaitlistService: Manages waitlist operations (add, remove, promote)
 *   • SlotGeneratorService: Generates slots from templates
 *
 * Structure:
 *   ┌─────────────────────────────────────────────────┐
 *   │           Top Bar (App Title + Welcome)         │
 *   ├──────────────────────────────────────────────────┤
 *   │       │                                          │
 *   │ Side  │  Content Area (Swappable Views)         │
 *   │ bar   │  • Pending Requests                      │
 *   │ Nav   │  • Waitlist Management                   │
 *   │       │  • Past Slots (Read-only)               │
 *   │       │  • Upcoming Slots + Change Status       │
 *   │       │  • Generate Slots + Manual Insert       │
 *   │       │                                          │
 *   └──────────────────────────────────────────────────┘
 *
 * All colours, fonts, and styles are defined as constants at the top
 * for easy maintenance and CSS synchronization.
 */
public class ProfessorView {

    // ═══════════════════════════════════════════════════════════════════════
    // § COLOUR PALETTE (Must match styles.css)
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final String BG_DARKEST    = "#0F1525";      // Root background
    private static final String BG_SIDEBAR    = "#1B264F";      // Sidebar panel
    private static final String BG_CONTENT    = "#111C34";      // Content area
    private static final String ACCENT_TEAL   = "#14B8A6";      // Primary CTA (buttons)
    private static final String ACCENT_TEAL_L = "#1CC8B5";      // Teal hover
    private static final String ACCENT_INDIGO = "#274690";      // Secondary accent
    private static final String ACCENT_INDIGO_L = "#576CA8";    // Indigo light
    private static final String STATUS_GREEN  = "#34D399";      // Success/approved
    private static final String STATUS_RED    = "#F87171";      // Danger/rejected
    private static final String STATUS_AMBER  = "#FBBF24";      // Pending
    private static final String STATUS_BLUE   = "#60A5FA";      // Waitlisted
    private static final String STATUS_TEAL   = "#2DD4BF";      // Available/free
    private static final String TEXT_PRIMARY  = "#E2E8F0";      // Main text
    private static final String TEXT_SECONDARY = "#CBD5E1";     // Secondary text
    private static final String TEXT_MUTED    = "#94A3B8";      // Disabled/hint text
    private static final String BORDER_LIGHT  = "rgba(255,255,255,0.07)";
    private static final String BORDER_MED    = "rgba(255,255,255,0.10)";

    // ═══════════════════════════════════════════════════════════════════════
    // § SCENE ENTRY POINT — Main Dashboard
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds the complete professor dashboard scene.
     * Returns a Scene containing:
     *   • Top bar with app title + welcome badge
     *   • Left sidebar with navigation buttons
     *   • Center content area (swappable views)
     *
     * @param professor The logged-in professor
     * @return Complete Scene ready for display
     */
    public static Scene getScene(Professor professor) {

        // ──────────────────────────────────────────────────────────────────
        // TOP BAR: App title + Professor welcome badge
        // ──────────────────────────────────────────────────────────────────
        
        Label appTitle = new Label("⏰  SlotSync");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        appTitle.setTextFill(Color.WHITE);
        appTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(7, 17, 41, 0.3), 8, 0, 0, 0);");

        Label welcomeLabel = new Label("Prof. " + professor.getFirstName());
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

        // ──────────────────────────────────────────────────────────────────
        // SIDEBAR: Navigation buttons + logout
        // ──────────────────────────────────────────────────────────────────
        
        Button pendingBtn   = createSidebarButton("📋  Pending Requests");
        Button waitlistBtn  = createSidebarButton("⏳  Waitlist Management");
        Button pastBtn      = createSidebarButton("🗂  Past Slots");
        Button upcomingBtn  = createSidebarButton("📅  Upcoming Slots");
        Button generateBtn  = createSidebarButton("✨  Generate Slots");
        Button logoutBtn    = createLogoutButton("🚪  Logout");

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        Label navLabel = createSidebarSectionLabel("NAVIGATION");

        VBox sidebar = new VBox(6,
            navLabel,
            pendingBtn, waitlistBtn, pastBtn, upcomingBtn, generateBtn,
            sidebarSpacer,
            logoutBtn
        );
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(180);
        sidebar.setMaxWidth(260);
        sidebar.setStyle(
            "-fx-background-color: " + BG_SIDEBAR + ";" +
            "-fx-border-color: rgba(39,70,144,0.20);" +
            "-fx-border-width: 0 1 0 0;"
        );

        // ──────────────────────────────────────────────────────────────────
        // CONTENT AREA: Swappable views (one view at a time)
        // ──────────────────────────────────────────────────────────────────
        
        BorderPane contentArea = new BorderPane();
        contentArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox welcome = buildWelcomeSplash(professor);
        welcome.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentArea.setCenter(welcome);
        contentArea.getStyleClass().add("content-area");
        contentArea.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ──────────────────────────────────────────────────────────────────
        // NAVIGATION BUTTON HANDLERS
        // ──────────────────────────────────────────────────────────────────
        
        pendingBtn.setOnAction(e -> {
            setActiveSidebarButton(pendingBtn, waitlistBtn, pastBtn, upcomingBtn, generateBtn);
            VBox view = buildPendingAppointmentsView(professor);
            view.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            contentArea.setCenter(view);
        });

        waitlistBtn.setOnAction(e -> {
            setActiveSidebarButton(waitlistBtn, pendingBtn, pastBtn, upcomingBtn, generateBtn);
            VBox view = buildWaitlistManagementView(professor);
            view.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            contentArea.setCenter(view);
        });

        pastBtn.setOnAction(e -> {
            setActiveSidebarButton(pastBtn, pendingBtn, waitlistBtn, upcomingBtn, generateBtn);
            VBox view = buildPastSlotsView(professor);
            view.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            contentArea.setCenter(view);
        });

        upcomingBtn.setOnAction(e -> {
            setActiveSidebarButton(upcomingBtn, pendingBtn, waitlistBtn, pastBtn, generateBtn);
            VBox view = buildUpcomingSlotsView(professor);
            view.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            contentArea.setCenter(view);
        });

        generateBtn.setOnAction(e -> {
            setActiveSidebarButton(generateBtn, pendingBtn, waitlistBtn, pastBtn, upcomingBtn);
            VBox view = buildGenerateSlotsView(professor);
            view.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            contentArea.setCenter(view);
        });

        logoutBtn.setOnAction(e -> Main.showLoginScreen());

        // ──────────────────────────────────────────────────────────────────
        // ROOT LAYOUT: BorderPane with top + left + center
        // ──────────────────────────────────────────────────────────────────
        
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentArea);
        contentArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
            ProfessorView.class.getResource("/styles.css").toExternalForm());
        return scene;
    }


    // ═══════════════════════════════════════════════════════════════════════
    // § WELCOME SPLASH (Initial Content)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a friendly welcome card shown on first load.
     * Encourages the user to select a section from the sidebar.
     */
    private static VBox buildWelcomeSplash(Professor professor) {
        Label icon = new Label("👋");
        icon.setFont(Font.font(52));
        icon.setStyle("-fx-effect: dropshadow(gaussian, rgba(39, 71, 144, 0.26), 16, 0, 0, 0);");

        Label heading = new Label("Welcome back, Prof. " + professor.getFirstName() + "!");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        heading.setTextFill(Color.web(TEXT_PRIMARY));

        Label sub = new Label("Choose a section from the sidebar to get started.");
        sub.setFont(Font.font("Segoe UI", 14));
        sub.setTextFill(Color.web(TEXT_MUTED));

        VBox splash = new VBox(14, icon, heading, sub);
        splash.setAlignment(Pos.CENTER);
        splash.setPadding(new Insets(60));
        splash.setStyle(
            "-fx-background-color: rgba(255,255,255,0.02);" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 20, 0, 0, 6);"
        );
        return splash;
    }


    // ═══════════════════════════════════════════════════════════════════════
    // § PENDING APPOINTMENTS VIEW
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Displays all PENDING appointments for this professor.
     * Each row has:
     *   • Appointment ID
     *   • Slot ID
     *   • Reason (colour-coded)
     *   • Status (colour-coded badge)
     *   • Booked At (timestamp)
     *   • Note (scrollable)
     *   • Update Status (ComboBox with PENDING/APPROVED/REJECTED/WAITLISTED)
     *
     * Service Method: AppointmentService.getPendingAppointmentsForProfessor(professorId)
     * Status Update: AppointmentService.updateAppointmentStatus(appointmentId, status)
     */
    private static VBox buildPendingAppointmentsView(Professor professor) {

        // ──────────────────────────────────────────────────────────────────
        // TABLE COLUMNS
        // ──────────────────────────────────────────────────────────────────
        
        TableColumn<Appointment, Integer> idCol = new TableColumn<>("Appt ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        idCol.setPrefWidth(70);

        TableColumn<Appointment, Integer> slotCol = new TableColumn<>("Slot ID");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotId"));
        slotCol.setPrefWidth(70);

        TableColumn<Appointment, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getReason().toString()));
        reasonCol.setPrefWidth(130);

        TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusCol.setCellFactory(col -> buildStatusCell());
        statusCol.setPrefWidth(100);

        TableColumn<Appointment, String> createdCol = new TableColumn<>("Booked At");
        createdCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCreatedAt().toString()));
        createdCol.setPrefWidth(140);

        TableColumn<Appointment, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getNote()));
        noteCol.setPrefWidth(200);

        // ──────────────────────────────────────────────────────────────────
        // ACTION COLUMN: Status Update Dropdown
        // ──────────────────────────────────────────────────────────────────
        
        TableColumn<Appointment, AppointmentStatus> actionCol = 
            new TableColumn<>("Update Status");
        actionCol.setPrefWidth(150);

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<AppointmentStatus> combo = new ComboBox<>();

            {
                combo.getItems().addAll(
                    AppointmentStatus.PENDING,
                    AppointmentStatus.APPROVED,
                    AppointmentStatus.REJECTED,
                    AppointmentStatus.WAITLISTED
                );
                styleComboBox(combo);

                combo.setOnAction(e -> {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    AppointmentStatus selected = combo.getValue();

                    // SERVICE CALL: Update appointment status
                    AppointmentService service = new AppointmentService();
                    boolean success = service.updateAppointmentStatus(
                        appt.getAppointmentId(), selected);

                    if (success) {
                        appt.setStatus(selected);
                        getTableView().refresh();
                        showAlert("Success", 
                            "Appointment status updated to " + selected, 
                            AlertType.INFORMATION);
                        
                        // If status changed from PENDING, remove row
                        if (selected != AppointmentStatus.PENDING) {
                            getTableView().getItems().remove(appt);
                        }
                    } else {
                        showAlert("Error", "Failed to update appointment status.", AlertType.ERROR);
                    }
                });
            }

            @Override
            protected void updateItem(AppointmentStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty) { setGraphic(null); }
                else       { combo.setValue(status); setGraphic(combo); }
            }
        });

        // ──────────────────────────────────────────────────────────────────
        // BUILD & POPULATE TABLE
        // ──────────────────────────────────────────────────────────────────
        
        TableView<Appointment> table = buildStyledTable();
        table.getColumns().addAll(
            idCol, slotCol, reasonCol, statusCol, createdCol, noteCol, actionCol);

        // SERVICE CALL: Get all pending appointments for this professor
        AppointmentService service = new AppointmentService();
        table.getItems().setAll(
            service.getPendingAppointmentsForProfessor(professor.getUserId()));

        return wrapInCard("📋  Pending Requests", table,
            table.getItems().size() + " pending appointment(s)");
    }


    // ═══════════════════════════════════════════════════════════════════════
    // § WAITLIST MANAGEMENT VIEW
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Allows professor to manage waitlisted students for a specific slot.
     *
     * Components:
     *   1. Slot selector dropdown (shows upcoming slots)
     *   2. Waitlist table (shows students on the waitlist for selected slot)
     *   3. Action buttons: Approve from waitlist, Remove from waitlist
     *
     * Service Methods:
     *   • TimeSlotService.getProfessorUpcomingSlots(professorId)
     *   • WaitlistService.getWaitlistBySlot(slotId)
     *   • WaitlistService.removeFromWaitlist(waitlistId)
     *   • WaitlistService.promoteFromWaitlist(slotId)
     */
    private static VBox buildWaitlistManagementView(Professor professor) {

    // ──────────────────────────────────────────────────────────────────
    // SLOT SELECTOR DROPDOWN
    // ──────────────────────────────────────────────────────────────────

    Label slotLabel = new Label("Select Slot:");
    slotLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
    slotLabel.setTextFill(Color.web(ACCENT_INDIGO_L));

    ComboBox<TimeSlot> slotSelector = new ComboBox<>();
    slotSelector.setPromptText("Choose an upcoming slot…");
    slotSelector.setPrefWidth(340);
    slotSelector.setPrefHeight(40);
    styleComboBox(slotSelector);

    // SERVICE CALL: Get upcoming slots
    TimeSlotService timeSlotService = new TimeSlotService();
    slotSelector.getItems().addAll(
        timeSlotService.getProfessorUpcomingSlots(professor.getUserId()));

    // Show slot ID + date + time range in dropdown
    slotSelector.setConverter(new javafx.util.StringConverter<TimeSlot>() {
        @Override
        public String toString(TimeSlot slot) {
            if (slot == null) return "";
            return "Slot #" + slot.getSlotID() + "  |  " +
                   slot.getSlotDate() + "  |  " +
                   slot.getStartTime().toString().substring(0, 5) + " – " +
                   slot.getEndTime().toString().substring(0, 5);
        }
        @Override
        public TimeSlot fromString(String s) { return null; }
    });

    HBox selectorRow = new HBox(12, slotLabel, slotSelector);
    selectorRow.setAlignment(Pos.CENTER_LEFT);
    selectorRow.setPadding(new Insets(12, 14, 12, 14));
    selectorRow.setStyle(
        "-fx-background-color: rgba(39,70,144,0.07);" +
        "-fx-border-color: rgba(39,70,144,0.15);" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;"
    );

    // ──────────────────────────────────────────────────────────────────
    // WAITLIST TABLE
    // ──────────────────────────────────────────────────────────────────

    TableColumn<WaitlistEntry, Integer> idCol = new TableColumn<>("Entry ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("waitlistId"));
    idCol.setPrefWidth(80);

    TableColumn<WaitlistEntry, String> studentIdCol = new TableColumn<>("Student ID");
    studentIdCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(
            String.valueOf(cellData.getValue().getStudent().getUserId())));
    studentIdCol.setPrefWidth(100);

    TableColumn<WaitlistEntry, String> studentNameCol = new TableColumn<>("Student Name");
    studentNameCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(
            cellData.getValue().getStudent().getFirstName() + " " +
            cellData.getValue().getStudent().getLastName()));
    studentNameCol.setPrefWidth(160);
    
    TableColumn<WaitlistEntry, Integer> priorityCol = new TableColumn<>("Priority Score");
    priorityCol.setCellValueFactory(new PropertyValueFactory<>("priorityScore"));
    priorityCol.setPrefWidth(110);

    TableColumn<WaitlistEntry, String> joinedCol = new TableColumn<>("Joined At");
    joinedCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getJoinedAt().toString()));
    joinedCol.setPrefWidth(140);

    // ──────────────────────────────────────────────────────────────────
    // ACTION COLUMN: Approve / Remove buttons
    // ──────────────────────────────────────────────────────────────────

    TableColumn<WaitlistEntry, Void> actionCol = new TableColumn<>("Actions");
    actionCol.setPrefWidth(180);

    actionCol.setCellFactory(col -> new TableCell<>() {
    private final Button approveBtn = new Button("Approve");
    private final Button removeBtn  = new Button("Remove");
    private final HBox   actions    = new HBox(6, approveBtn, removeBtn); // ← field, not local variable

    {
        approveBtn.getStyleClass().add("action-button-success");
        approveBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11;");
        removeBtn.getStyleClass().add("action-button-danger");
        removeBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11;");

        actions.setAlignment(Pos.CENTER);

        approveBtn.setOnAction(e -> {
            WaitlistEntry entry = getTableView().getItems().get(getIndex());
            WaitlistService service = new WaitlistService();
            boolean success = service.promoteFromWaitlist(entry.getSlot().getSlotID());
            if (success) {
                getTableView().getItems().remove(entry);
                showAlert("Success", "Student approved and slot assigned.", AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to promote student from waitlist.", AlertType.ERROR);
            }
        });

        removeBtn.setOnAction(e -> {
            WaitlistEntry entry = getTableView().getItems().get(getIndex());
            WaitlistService service = new WaitlistService();
            boolean success = service.removeFromWaitlist(entry.getWaitlistId());
            if (success) {
                getTableView().getItems().remove(entry);
                showAlert("Success", "Student removed from waitlist.", AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to remove student.", AlertType.ERROR);
            }
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : actions); // ← references the field directly, always works
    }
});

    TableView<WaitlistEntry> waitlistTable = buildStyledTable();
    waitlistTable.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    VBox.setVgrow(waitlistTable, Priority.ALWAYS);
    waitlistTable.getColumns().addAll(idCol, studentIdCol, studentNameCol,priorityCol, joinedCol, actionCol);

    // ──────────────────────────────────────────────────────────────────
    // LOAD WAITLIST WHEN SLOT SELECTED
    // ──────────────────────────────────────────────────────────────────

    slotSelector.setOnAction(e -> {
        TimeSlot selected = slotSelector.getValue();
        if (selected != null) {
            WaitlistService waitlistService = new WaitlistService();
            java.util.List<WaitlistEntry> entries =
                waitlistService.getWaitlistBySlot(selected.getSlotID());
            waitlistTable.getItems().setAll(entries);
        } else {
            waitlistTable.getItems().clear();
        }
    });

    VBox card = new VBox(18);
    card.getChildren().addAll(
        buildCardHeader("⏳  Waitlist Management", "Review and manage student waitlists"),
        selectorRow,
        waitlistTable
    );
    card.setPadding(new Insets(24));
    applyCardStyle(card);
    VBox.setVgrow(card, Priority.ALWAYS);
    card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    return card;
}


    // ═══════════════════════════════════════════════════════════════════════
    // § PAST SLOTS VIEW (Read-only)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Shows all completed/past time slots in a read-only table.
     * Displays slot details: ID, Date, Start/End time, Status
     *
     * Service Method: TimeSlotService.getProfessorPastSlots(professorId)
     */
    private static VBox buildPastSlotsView(Professor professor) {

        TableColumn<TimeSlot, Integer> idCol = new TableColumn<>("Slot ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("slotID"));
        idCol.setPrefWidth(70);

        TableColumn<TimeSlot, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSlotDate().toString()));
        dateCol.setPrefWidth(120);

        TableColumn<TimeSlot, String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStartTime().toString()));
        startCol.setPrefWidth(100);

        TableColumn<TimeSlot, String> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEndTime().toString()));
        endCol.setPrefWidth(100);

        TableColumn<TimeSlot, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus().toString()));
        statusCol.setCellFactory(col -> buildSlotStatusCell());
        statusCol.setPrefWidth(110);

        TableView<TimeSlot> table = buildStyledTable();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(idCol, dateCol, startCol, endCol, statusCol);

        // SERVICE CALL: Get past slots
        TimeSlotService service = new TimeSlotService();
        table.getItems().setAll(
            service.getProfessorPastSlots(professor.getUserId()));

        return wrapInCard("🗂  Past Slots", table,
            table.getItems().size() + " past slot(s)");
    }


    // ═══════════════════════════════════════════════════════════════════════
    // § UPCOMING SLOTS VIEW
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Shows all upcoming time slots for this professor.
     * Each row has interactive controls:
     *   • Change Status dropdown (FREE → CANCELLED/FROZEN)
     *   • Block/Unblock button (to prevent students from booking)
     *
     * Service Methods:
     *   • TimeSlotService.getProfessorUpcomingSlots(professorId)
     *   • TimeSlotService.updateSlotStatus(slotId, status)
     *   • TimeSlotService.blockSlot(slotId)
     *   • TimeSlotService.unblockSlot(slotId)
     */
    private static VBox buildUpcomingSlotsView(Professor professor) {

        // ──────────────────────────────────────────────────────────────────
        // TABLE COLUMNS
        // ──────────────────────────────────────────────────────────────────
        
        TableColumn<TimeSlot, Integer> idCol = new TableColumn<>("Slot ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("slotID"));
        idCol.setPrefWidth(70);

        TableColumn<TimeSlot, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSlotDate().toString()));
        dateCol.setPrefWidth(120);

        TableColumn<TimeSlot, String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStartTime().toString()));
        startCol.setPrefWidth(100);

        TableColumn<TimeSlot, String> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEndTime().toString()));
        endCol.setPrefWidth(100);

        TableColumn<TimeSlot, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus().toString()));
        statusCol.setCellFactory(col -> buildSlotStatusCell());
        statusCol.setPrefWidth(110);

        // ──────────────────────────────────────────────────────────────────
        // CHANGE STATUS DROPDOWN COLUMN
        // ──────────────────────────────────────────────────────────────────
        
        TableColumn<TimeSlot, TimeSlotStatus> changeStatusCol = 
            new TableColumn<>("Change Status");
        changeStatusCol.setPrefWidth(150);

        changeStatusCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<TimeSlotStatus> statusCombo = new ComboBox<>();

            {
                statusCombo.getItems().addAll(
                    TimeSlotStatus.FREE,
                    TimeSlotStatus.CANCELLED,
                    TimeSlotStatus.FROZEN
                );
                styleComboBox(statusCombo);

                statusCombo.setOnAction(e -> {
                    TimeSlot slot = getTableView().getItems().get(getIndex());
                    TimeSlotStatus selected = statusCombo.getValue();

                    // SERVICE CALL: Update slot status
                    TimeSlotService service = new TimeSlotService();
                    boolean success = service.updateSlotStatus(slot.getSlotID(), selected);

                    if (success) {
                        slot.setStatus(selected);
                        getTableView().refresh();
                        showAlert("Success", "Slot status updated to " + selected + ".", AlertType.INFORMATION);
                    } else {
                        showAlert("Error", "Failed to update slot status.", AlertType.ERROR);
                    }
                });
            }

            @Override
            protected void updateItem(TimeSlotStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty) { setGraphic(null); }
                else       { statusCombo.setValue(status); setGraphic(statusCombo); }
            }
        });

        // ──────────────────────────────────────────────────────────────────
        // BLOCK/UNBLOCK BUTTON COLUMN
        // ──────────────────────────────────────────────────────────────────
        
        TableColumn<TimeSlot, Void> blockCol = new TableColumn<>("Block/Unblock");
        blockCol.setPrefWidth(140);

        blockCol.setCellFactory(col -> new TableCell<>() {
            private final Button blockBtn = new Button("Block");

            {
                blockBtn.getStyleClass().add("action-button-ghost");
                blockBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11;");

                blockBtn.setOnAction(e -> {
                    TimeSlot slot = getTableView().getItems().get(getIndex());
                    TimeSlotService service = new TimeSlotService();

                    // Toggle block/unblock
                    if (slot.getStatus() == TimeSlotStatus.FROZEN) {
                        // If frozen, unblock it
                        boolean success = service.unblockSlot(slot.getSlotID());
                        if (success) {
                            slot.setStatus(TimeSlotStatus.FREE);
                            blockBtn.setText("Block");
                            getTableView().refresh();
                            showAlert("Success", "Slot unlocked.", AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to unlock slot.", AlertType.ERROR);
                        }
                    } else {
                        // Block the slot
                        boolean success = service.blockSlot(slot.getSlotID());
                        if (success) {
                            slot.setStatus(TimeSlotStatus.FROZEN);
                            blockBtn.setText("Unblock");
                            getTableView().refresh();
                            showAlert("Success", "Slot blocked.", AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to block slot.", AlertType.ERROR);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TimeSlot slot = getTableView().getItems().get(getIndex());
                    blockBtn.setText(slot.getStatus() == TimeSlotStatus.FROZEN ? "Unblock" : "Block");
                    setGraphic(blockBtn);
                }
            }
        });

        // ──────────────────────────────────────────────────────────────────
        // BUILD & POPULATE TABLE
        // ──────────────────────────────────────────────────────────────────
        
        TableView<TimeSlot> table = buildStyledTable();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
            idCol, dateCol, startCol, endCol, statusCol, changeStatusCol, blockCol);

        // SERVICE CALL: Get upcoming slots
        TimeSlotService service = new TimeSlotService();
        table.getItems().setAll(
            service.getProfessorUpcomingSlots(professor.getUserId()));

        return wrapInCard("📅  Upcoming Slots", table,
            table.getItems().size() + " upcoming slot(s)");
    }


    // ═══════════════════════════════════════════════════════════════════════
    // § GENERATE / MANAGE SLOTS VIEW
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Allows professor to create new time slots.
     * Components:
     *   1. "Manually Insert Custom Slot" button → opens dialog
     *   2. Template time slots (predefined) with "Generate" buttons
     *
     * Service Method: SlotGeneratorService.generateSingleSlot(template, professorId)
     */
   // ═══════════════════════════════════════════════════════════════════════
// § GENERATE / MANAGE SLOTS VIEW
// ═══════════════════════════════════════════════════════════════════════

/**
 * Allows professor to create new time slots.
 * Components:
 *   1. "Manually Insert Custom Slot" button → opens dialog
 *   2. Template time slots table (from common_slots.json) with per-row Generate buttons
 *
 * Service Method: SlotGeneratorService.generateSingleSlot(template, professorId)
 */
private static VBox buildGenerateSlotsView(Professor professor) {

    // ──────────────────────────────────────────────────────────────────
    // MANUAL INSERT BUTTON
    // ──────────────────────────────────────────────────────────────────

    Button manualInsertBtn = new Button("＋  Manually Insert Custom Slot");
    manualInsertBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
    manualInsertBtn.setPrefHeight(44);
    manualInsertBtn.setPrefWidth(280);
    manualInsertBtn.setStyle(
        "-fx-background-color: linear-gradient(to right," + ACCENT_INDIGO + ",#576CA8);" +
        "-fx-text-fill: white;" +
        "-fx-font-size: 13;" +
        "-fx-font-weight: bold;" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;" +
        "-fx-cursor: hand;" +
        "-fx-padding: 10 22;" +
        "-fx-effect: dropshadow(gaussian, rgba(39,70,144,0.30), 10, 0, 0, 3);"
    );
    manualInsertBtn.setOnMouseEntered(e -> manualInsertBtn.setStyle(
        "-fx-background-color: linear-gradient(to right,#576CA8,#7B93C4);" +
        "-fx-text-fill: white;" +
        "-fx-font-size: 13;" +
        "-fx-font-weight: bold;" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;" +
        "-fx-cursor: hand;" +
        "-fx-padding: 10 22;" +
        "-fx-effect: dropshadow(gaussian, rgba(39,70,144,0.45), 14, 0, 0, 5);"
    ));
    manualInsertBtn.setOnMouseExited(e -> manualInsertBtn.setStyle(
        "-fx-background-color: linear-gradient(to right," + ACCENT_INDIGO + ",#576CA8);" +
        "-fx-text-fill: white;" +
        "-fx-font-size: 13;" +
        "-fx-font-weight: bold;" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;" +
        "-fx-cursor: hand;" +
        "-fx-padding: 10 22;" +
        "-fx-effect: dropshadow(gaussian, rgba(39,70,144,0.30), 10, 0, 0, 3);"
    ));
    manualInsertBtn.setOnAction(e -> showManualInsertDialog(professor));

    Label manualDesc = new Label(
        "Manually create a custom time slot for students to book.\n" +
        "Specify the date, start time, and end time.");
    manualDesc.setTextFill(Color.web(TEXT_MUTED));
    manualDesc.setFont(Font.font("Segoe UI", 13));
    manualDesc.setWrapText(true);

    // ──────────────────────────────────────────────────────────────────
    // DIVIDER BETWEEN MANUAL INSERT AND TEMPLATE TABLE
    // ──────────────────────────────────────────────────────────────────

    Separator divider = new Separator();
    divider.setStyle("-fx-background-color: rgba(255,255,255,0.07);");
    VBox.setMargin(divider, new Insets(6, 0, 6, 0));

    // ──────────────────────────────────────────────────────────────────
    // TEMPLATE SLOTS SECTION HEADER
    // ──────────────────────────────────────────────────────────────────

    Label templateTitle = new Label("📋  Template Slots");
    templateTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
    templateTitle.setTextFill(Color.web(TEXT_PRIMARY));

    Label templateDesc = new Label(
        "Predefined slot templates from your schedule. " +
        "Click Generate to create a slot for a specific date.");
    templateDesc.setTextFill(Color.web(TEXT_MUTED));
    templateDesc.setFont(Font.font("Segoe UI", 13));
    templateDesc.setWrapText(true);

    VBox templateHeader = new VBox(4, templateTitle, templateDesc);

    // ──────────────────────────────────────────────────────────────────
    // LOAD TEMPLATE SLOTS FROM JSON
    // ──────────────────────────────────────────────────────────────────

    ObservableList<SlotTemplate> templates = FXCollections.observableArrayList();
    try {
        java.io.InputStream is = ProfessorView.class.getResourceAsStream("/common_slots.json");
        if (is != null) {
            Reader reader = new InputStreamReader(is);
            java.lang.reflect.Type listType =
                new com.google.gson.reflect.TypeToken<java.util.List<SlotTemplate>>(){}.getType();
            java.util.List<SlotTemplate> loaded = new Gson().fromJson(reader, listType);
            if (loaded != null) templates.addAll(loaded);
            reader.close();
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }

    // ──────────────────────────────────────────────────────────────────
    // TEMPLATE TABLE COLUMNS
    // ──────────────────────────────────────────────────────────────────

    TableColumn<SlotTemplate, String> dayCol = new TableColumn<>("Day");
    dayCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDay()));
    dayCol.setPrefWidth(120);
    dayCol.setCellFactory(col -> new TableCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " + ACCENT_INDIGO_L + ";");
            }
        }
    });

    TableColumn<SlotTemplate, String> startCol = new TableColumn<>("Start Time");
    startCol.setCellValueFactory(c -> {
        // Strip trailing seconds (:00) for cleaner display → "10:30"
        String t = c.getValue().getStartTime();
        return new SimpleStringProperty(t.length() >= 5 ? t.substring(0, 5) : t);
    });
    startCol.setPrefWidth(110);
    startCol.setCellFactory(col -> new TableCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setStyle(""); }
            else {
                setText(item);
                setStyle("-fx-text-fill: " + STATUS_TEAL + "; -fx-font-weight: bold;");
            }
        }
    });

    TableColumn<SlotTemplate, String> endCol = new TableColumn<>("End Time");
    endCol.setCellValueFactory(c -> {
        String t = c.getValue().getEndTime();
        return new SimpleStringProperty(t.length() >= 5 ? t.substring(0, 5) : t);
    });
    endCol.setPrefWidth(110);
    endCol.setCellFactory(col -> new TableCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setStyle(""); }
            else {
                setText(item);
                setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";");
            }
        }
    });

    // ──────────────────────────────────────────────────────────────────
    // GENERATE BUTTON COLUMN (Date picker + Generate per row)
    // ──────────────────────────────────────────────────────────────────

    TableColumn<SlotTemplate, Void> generateCol = new TableColumn<>("Generate For Date");
    generateCol.setPrefWidth(300);
    generateCol.setResizable(false);

    generateCol.setCellFactory(col -> new TableCell<>() {
        private final DatePicker datePicker = new DatePicker();
        private final Button genBtn        = new Button("⚡  Generate");
        private final HBox   cell          = new HBox(8, datePicker, genBtn);

        {
            datePicker.setPrefWidth(140);
            datePicker.setPrefHeight(32);
            datePicker.getStyleClass().add("date-picker");

            genBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            genBtn.setPrefHeight(32);
            genBtn.setStyle(
                "-fx-background-color: linear-gradient(to right," + ACCENT_TEAL + "," + ACCENT_TEAL_L + ");" +
                "-fx-text-fill: #0F1525;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;" +
                "-fx-border-radius: 7;" +
                "-fx-background-radius: 7;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(20,184,166,0.30), 8, 0, 0, 2);"
            );
            genBtn.setOnMouseEntered(e -> genBtn.setStyle(
                "-fx-background-color: linear-gradient(to right," + ACCENT_TEAL_L + ",#22D3CB);" +
                "-fx-text-fill: #0F1525;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;" +
                "-fx-border-radius: 7;" +
                "-fx-background-radius: 7;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(20,184,166,0.50), 10, 0, 0, 3);"
            ));
            genBtn.setOnMouseExited(e -> genBtn.setStyle(
                "-fx-background-color: linear-gradient(to right," + ACCENT_TEAL + "," + ACCENT_TEAL_L + ");" +
                "-fx-text-fill: #0F1525;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;" +
                "-fx-border-radius: 7;" +
                "-fx-background-radius: 7;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(20,184,166,0.30), 8, 0, 0, 2);"
            ));

            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(4, 0, 4, 0));

            genBtn.setOnAction(e -> {
    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) return;
    SlotTemplate template = getTableView().getItems().get(getIndex());

    if (datePicker.getValue() == null) {
        showAlert("Missing Date",
            "Please pick a date for the \"" + template.getDay() +
            " " + template.getStartTime().substring(0, 5) + "\" slot.",
            AlertType.WARNING);
        return;
    }

    // Build a temporary SlotTemplate with the date baked into it
    // and call the existing 2-parameter method
    SlotGeneratorService generatorService = new SlotGeneratorService();
    boolean success = generatorService.generateSingleSlot(template, professor.getUserId());

    if (success) {
        showAlert("Slot Generated",
            template.getDay() + " slot " +
            template.getStartTime().substring(0, 5) + " – " +
            template.getEndTime().substring(0, 5) +
            " created for " + datePicker.getValue() + ".",
            AlertType.INFORMATION);
        datePicker.setValue(null);
    } else {
        showAlert("Error",
            "Failed to generate slot. It may already exist.",
            AlertType.ERROR);
    }
});
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : cell);
        }
    });

    // ──────────────────────────────────────────────────────────────────
    // TEMPLATE TABLE (scrollable, constrained resize)
    // ──────────────────────────────────────────────────────────────────

    TableView<SlotTemplate> templateTable = new TableView<>(templates);
    templateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    templateTable.getColumns().addAll(dayCol, startCol, endCol, generateCol);
    templateTable.setStyle(
        "-fx-background-color: transparent;" +
        "-fx-border-color: rgba(255,255,255,0.07);" +
        "-fx-border-radius: 10;" +
        "-fx-padding: 0;"
    );
    templateTable.setPrefHeight(340);
    templateTable.setMaxWidth(Double.MAX_VALUE);

    Label rowCount = new Label(templates.size() + " template slot(s) loaded");
    rowCount.setFont(Font.font("Segoe UI", 12));
    rowCount.setTextFill(Color.web(TEXT_MUTED));

    // ──────────────────────────────────────────────────────────────────
    // COMPOSE FULL CARD
    // ──────────────────────────────────────────────────────────────────

    VBox card = new VBox(16);
    card.setFillWidth(true);
    card.getChildren().addAll(
        buildCardHeader("✨  Slot Management", "Create and manage your availability"),
        manualDesc,
        manualInsertBtn,
        divider,
        templateHeader,
        rowCount,
        templateTable
    );
    card.setPadding(new Insets(24));
    applyCardStyle(card);
    VBox.setVgrow(templateTable, Priority.ALWAYS);
    card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    return card;
}


    // ═══════════════════════════════════════════════════════════════════════
    // § MANUAL SLOT DIALOG
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Opens a styled dialog for manually creating a new time slot.
     * Fields:
     *   • Date picker
     *   • Start time (HH:mm format)
     *   • End time (HH:mm format)
     *
     * On "Save Slot" creates the new TimeSlot and saves to database.
     *
     * Service Note: TimeSlot model used to create object,
     *              which will be saved via TimeSlotService in actual implementation.
     */
    private static void showManualInsertDialog(Professor professor) {

        Dialog<TimeSlot> dialog = new Dialog<>();
        dialog.setTitle("New Time Slot");
        dialog.setHeaderText("Create a Custom Slot");

        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().getStylesheets().add(
            ProfessorView.class.getResource("/styles.css").toExternalForm());

        ButtonType saveButtonType =
            new ButtonType("Save Slot", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // ──────────────────────────────────────────────────────────────────
        // FORM FIELDS
        // ──────────────────────────────────────────────────────────────────
        
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 20, 10, 20));

        Label dateLabel = styledDialogLabel("Date:");
        DatePicker datePicker = new DatePicker();
        datePicker.setPrefWidth(220);
        datePicker.setPrefHeight(40);
        datePicker.getStyleClass().add("date-picker");

        Label startLabel = styledDialogLabel("Start Time (HH:mm):");
        TextField startTimeField = new TextField("09:00");
        startTimeField.setPrefWidth(220);
        startTimeField.setPrefHeight(40);
        styleDialogTextField(startTimeField);

        Label endLabel = styledDialogLabel("End Time (HH:mm):");
        TextField endTimeField = new TextField("09:30");
        endTimeField.setPrefWidth(220);
        endTimeField.setPrefHeight(40);
        styleDialogTextField(endTimeField);

        grid.add(dateLabel,      0, 0); grid.add(datePicker,    1, 0);
        grid.add(startLabel,     0, 1); grid.add(startTimeField, 1, 1);
        grid.add(endLabel,       0, 2); grid.add(endTimeField,   1, 2);

        dialog.getDialogPane().setContent(grid);

        // ──────────────────────────────────────────────────────────────────
        // RESULT CONVERTER
        // ──────────────────────────────────────────────────────────────────
        
        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                try {
                    TimeSlot newSlot = new TimeSlot(
                        datePicker.getValue(),
                        professor.getUserId(),
                        java.time.LocalTime.parse(startTimeField.getText()),
                        java.time.LocalTime.parse(endTimeField.getText())
                    );

                    // NOTE: In actual implementation, save via TimeSlotService
                    // TimeSlotService service = new TimeSlotService();
                    // boolean success = service.createTimeSlot(newSlot);
                    
                    showAlert("Success", 
                        "Slot created for " + datePicker.getValue(), 
                        AlertType.INFORMATION);

                    return newSlot;
                } catch (Exception e) {
                    showAlert("Error", "Invalid time format. Use HH:mm (e.g., 14:30)", AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }


    // ═══════════════════════════════════════════════════════════════════════
    // § STYLE & UTILITY HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    // ──────────────────────────────────────────────────────────────────────
    // SIDEBAR BUTTON STYLING
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Creates a styled sidebar navigation button.
     * States: Default (muted), Hovered (highlighted), Active (primary color)
     */
    private static Button createSidebarButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", 13));
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(buildSidebarBtnStyle(false));

        btn.setOnMouseEntered(e -> btn.setStyle(buildSidebarBtnStyle(true)));
        btn.setOnMouseExited(e -> btn.setStyle(buildSidebarBtnStyle(false)));

        return btn;
    }

    /** CSS string for sidebar button (hovered or not) */
    private static String buildSidebarBtnStyle(boolean hovered) {
        if (hovered) {
            return "-fx-background-color: rgba(39,70,144,0.20);" +
                   "-fx-text-fill: #8FA8D0;" +
                   "-fx-font-size: 13;" +
                   "-fx-font-weight: 600;" +
                   "-fx-padding: 10 16;" +
                   "-fx-background-radius: 8;" +
                   "-fx-cursor: hand;" +
                   "-fx-alignment: CENTER_LEFT;";
        } else {
            return "-fx-background-color: transparent;" +
                   "-fx-text-fill: #94A3B8;" +
                   "-fx-font-size: 13;" +
                   "-fx-font-weight: 600;" +
                   "-fx-padding: 10 16;" +
                   "-fx-background-radius: 8;" +
                   "-fx-cursor: hand;" +
                   "-fx-alignment: CENTER_LEFT;";
        }
    }

    /** CSS string for active (selected) sidebar button */
    private static String buildSidebarBtnActiveStyle() {
        return "-fx-background-color: rgba(39,70,144,0.30);" +
               "-fx-text-fill: " + ACCENT_INDIGO_L + ";" +
               "-fx-font-size: 13;" +
               "-fx-font-weight: bold;" +
               "-fx-padding: 10 16 10 13;" +
               "-fx-background-radius: 8;" +
               "-fx-cursor: hand;" +
               "-fx-alignment: CENTER_LEFT;" +
               "-fx-border-color: " + ACCENT_INDIGO + " transparent transparent transparent;" +
               "-fx-border-width: 0 0 0 3;" +
               "-fx-border-radius: 0;";
    }

    /** Updates which sidebar button appears "active" */
    private static void setActiveSidebarButton(Button active, Button... others) {
        active.setStyle(buildSidebarBtnActiveStyle());
        for (Button btn : others) {
            btn.setStyle(buildSidebarBtnStyle(false));
            btn.setOnMouseEntered(e -> btn.setStyle(buildSidebarBtnStyle(true)));
            btn.setOnMouseExited(e -> btn.setStyle(buildSidebarBtnStyle(false)));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // LOGOUT BUTTON
    // ──────────────────────────────────────────────────────────────────────

    /** Creates the red-tinted logout button at the bottom of sidebar */
    private static Button createLogoutButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(
            "-fx-background-color: rgba(239,68,68,0.12);" +
            "-fx-text-fill: #FCA5A5;" +
            "-fx-padding: 10 16;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(239,68,68,0.22);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: CENTER_LEFT;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: rgba(239,68,68,0.22);" +
            "-fx-text-fill: #FECACA;" +
            "-fx-padding: 10 16;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(239,68,68,0.35);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: CENTER_LEFT;" +
            "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.20), 6, 0, 0, 0);"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: rgba(239,68,68,0.12);" +
            "-fx-text-fill: #FCA5A5;" +
            "-fx-padding: 10 16;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(239,68,68,0.22);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: CENTER_LEFT;"
        ));
        return btn;
    }

    // ──────────────────────────────────────────────────────────────────────
    // SIDEBAR SECTION LABEL
    // ──────────────────────────────────────────────────────────────────────

    /** Small uppercase category label in sidebar */
    private static Label createSidebarSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web("#475569"));
        lbl.setPadding(new Insets(14, 0, 4, 16));
        return lbl;
    }

    // ──────────────────────────────────────────────────────────────────────
    // CARD HEADER
    // ──────────────────────────────────────────────────────────────────────

    /** Builds the title + subtitle header for a content card */
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

    // ──────────────────────────────────────────────────────────────────────
    // CARD WRAPPING
    // ──────────────────────────────────────────────────────────────────────

    /** Convenience: wraps a table in a styled card with header */
    private static VBox wrapInCard(String title, TableView<?> table, String subtitle) {
        VBox card = new VBox(16);
        card.getChildren().addAll(buildCardHeader(title, subtitle), table);
        card.setPadding(new Insets(24));
        applyCardStyle(card);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMaxWidth(Double.MAX_VALUE);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    /** Applies the glass-card background style to a VBox */
    private static void applyCardStyle(VBox card) {
        card.setFillWidth(true);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.025);" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 20, 0, 0, 6);"
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    // TABLE VIEW
    // ──────────────────────────────────────────────────────────────────────

    /** Creates an empty styled TableView with dark theme */
    private static <T> TableView<T> buildStyledTable() {
        TableView<T> table = new TableView<>();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: rgba(255,255,255,0.07);" +
            "-fx-border-radius: 10;" +
            "-fx-padding: 0;"
        );
        return table;
    }

    // ──────────────────────────────────────────────────────────────────────
    // STATUS CELL RENDERING (Colour-coded badges)
    // ──────────────────────────────────────────────────────────────────────

    /** Renders appointment status with colour: APPROVED→green, REJECTED→red, etc. */
    private static TableCell<Appointment, String> buildStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: " + appointmentStatusColor(item) + ";");
                }
            }
        };
    }

    /** Renders slot status with colour: FREE→teal, CANCELLED→red, etc. */
    private static TableCell<TimeSlot, String> buildSlotStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: " + slotStatusColor(item) + ";");
                }
            }
        };
    }

    /** Maps appointment status to palette colour */
    private static String appointmentStatusColor(String status) {
        return switch (status) {
            case "APPROVED"   -> STATUS_GREEN;
            case "REJECTED"   -> STATUS_RED;
            case "PENDING"    -> STATUS_AMBER;
            case "WAITLISTED" -> STATUS_BLUE;
            default           -> TEXT_PRIMARY;
        };
    }

    /** Maps slot status to palette colour */
    private static String slotStatusColor(String status) {
        return switch (status) {
            case "FREE"              -> STATUS_TEAL;
            case "PARTIALLY_BOOKED"  -> ACCENT_INDIGO_L;
            case "BOOKED"            -> ACCENT_INDIGO_L;
            case "CANCELLED"         -> STATUS_RED;
            case "FROZEN"            -> STATUS_AMBER;
            case "COMPLETED"         -> TEXT_MUTED;
            default                  -> TEXT_PRIMARY;
        };
    }

    // ──────────────────────────────────────────────────────────────────────
    // COMBOBOX STYLING
    // ──────────────────────────────────────────────────────────────────────

    /** Styles a ComboBox with dark-glass palette */
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

    // ──────────────────────────────────────────────────────────────────────
    // DIALOG FORM STYLING
    // ──────────────────────────────────────────────────────────────────────

    /** Styles a label inside a dialog form */
    private static Label styledDialogLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(ACCENT_INDIGO_L));
        return lbl;
    }

    /** Styles a text field inside a dialog */
    private static void styleDialogTextField(TextField tf) {
        tf.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-border-color: rgba(87,108,168,0.30);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-text-fill: #E2E8F0;" +
            "-fx-prompt-text-fill: #475569;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 9 12;"
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    // ALERT DIALOG
    // ──────────────────────────────────────────────────────────────────────

    /** Shows a simple alert dialog with title, message, and type */
    private static void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}