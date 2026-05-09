package view;

import java.util.List;

import enums.AppointmentStatus;
import enums.TimeSlotStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import main.Main;
import model.Appointment;
import model.Professor;
import model.TimeSlot;
import model.WaitlistEntry;
import service.AppointmentService;
import service.TimeSlotService;
import service.WaitlistService;

public class ProfessorView {

    public static Scene getScene(Professor professor) {
        // ── TOP BAR ───────────────────────────────
        Label appTitle = new Label("Appointment System");
        appTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Label welcomeLabel = new Label("Welcome, Prof. " + professor.getFirstName());
        welcomeLabel.setFont(Font.font("Arial", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, appTitle, spacer, welcomeLabel);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #197c6f;");

        // ── SIDEBAR ───────────────────────────────
        Button pendingBtn   = new Button("Pending Requests");
        Button waitlistBtn  = new Button("Waitlist Management");
        Button pastBtn  = new Button("Past Slots");
        Button upcomingBtn  = new Button("Upcoming Slots");
        Button logoutBtn    = new Button("Logout");

        for (Button btn : new Button[]{
                pendingBtn, waitlistBtn, pastBtn, upcomingBtn, logoutBtn}) {
            btn.setPrefWidth(180);
            btn.setPrefHeight(35);
            btn.setFont(Font.font("Arial", 13));
        }

        VBox sidebar = new VBox(10,
            pendingBtn, waitlistBtn, pastBtn, upcomingBtn, logoutBtn);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #addfd5;");

        // ── MAIN CONTENT AREA ─────────────────────
        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        contentArea.getChildren().add(new Label("Professor Dashboard"));

        // ── SIDEBAR BUTTON ACTIONS ────────────────
        
        pendingBtn.setOnAction(e ->
            contentArea.getChildren().setAll(buildPendingAppointmentsView(professor)));
        waitlistBtn.setOnAction(e ->
            contentArea.getChildren().setAll(buildWaitlistManagementView(professor)));
        pastBtn.setOnAction(e ->
            contentArea.getChildren().setAll(buildPastWeekSlotsView(professor)));
        upcomingBtn.setOnAction(e ->
            contentArea.getChildren().setAll(buildUpcomingSlotsView(professor)));
        logoutBtn.setOnAction(e ->
            Main.showLoginScreen());

        // ── ROOT LAYOUT ──────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        return new Scene(root, 800, 600);
    }


    // helper method to make table for pending appointments
    private static TableView<Appointment> buildPendingAppointmentsView(Professor professor) {
    TableView<Appointment> table = new TableView<>();

    // Define columns
    TableColumn<Appointment, Integer> idCol = new TableColumn<>("Appt ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));

    TableColumn<Appointment, Integer> slotCol = new TableColumn<>("Slot ID");
    slotCol.setCellValueFactory(new PropertyValueFactory<>("slotId"));

    TableColumn<Appointment, String> reasonCol = new TableColumn<>("Reason");
    reasonCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getReason().toString()));

    TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getStatus().toString()));

    TableColumn<Appointment, String> createdCol = new TableColumn<>("Booked At");
    createdCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getCreatedAt().toString()));

    TableColumn<Appointment, String> noteCol = new TableColumn<>("Note");
    noteCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getNote()));

    TableColumn<Appointment, AppointmentStatus> actionCol = new TableColumn<>("Update Status");
    actionCol.setCellFactory(col -> new TableCell<>() {
    private final ComboBox<AppointmentStatus> combo = new ComboBox<>();

    {
        combo.getItems().addAll(AppointmentStatus.PENDING,
                                AppointmentStatus.APPROVED,
                                AppointmentStatus.REJECTED,
                                AppointmentStatus.WAITLISTED);

        combo.setOnAction(e -> {
            Appointment appt = getTableView().getItems().get(getIndex());
            AppointmentStatus selected = combo.getValue();

            // Update in DB
            AppointmentService service = new AppointmentService();
            boolean success = service.updateAppointmentStatus(appt.getAppointmentId(), selected);

            if (success) {
                appt.setStatus(selected); // update local object
                getTableView().refresh(); // refresh table UI
            }
        });
    }

    @Override
    protected void updateItem(AppointmentStatus status, boolean empty) {
        super.updateItem(status, empty);
        if (empty) {
            setGraphic(null);
        } else {
            combo.setValue(status); // show current status
            setGraphic(combo);
        }
    }
    });


    // Add columns to table
    table.getColumns().addAll(idCol, slotCol, reasonCol, statusCol, createdCol, noteCol, actionCol);

    // Fetch data from service
    AppointmentService service = new AppointmentService();
    List<Appointment> pending = service.getPendingAppointmentsForProfessor(professor.getUserId());

    table.getItems().setAll(pending);

    
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    idCol.setPrefWidth(80);
    slotCol.setPrefWidth(80);
    reasonCol.setPrefWidth(150);
    statusCol.setPrefWidth(100);
    createdCol.setPrefWidth(180);
    noteCol.setPrefWidth(400); // long notes need more space

    table.setPrefWidth(800); // match your scene width



    return table;
}

    // helper method to make table for all past appointments
    private static TableView<TimeSlot> buildPastWeekSlotsView(Professor professor) {
    TableView<TimeSlot> table = new TableView<>();

    TableColumn<TimeSlot, Integer> idCol = new TableColumn<>("Slot ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("slotID"));

    TableColumn<TimeSlot, String> dateCol = new TableColumn<>("Date");
    dateCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getSlotDate().toString()));

    TableColumn<TimeSlot, String> startCol = new TableColumn<>("Start");
    startCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getStartTime().toString()));

    TableColumn<TimeSlot, String> endCol = new TableColumn<>("End");
    endCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getEndTime().toString()));

    TableColumn<TimeSlot, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getStatus().toString()));

    table.getColumns().addAll(idCol, dateCol, startCol, endCol, statusCol);

    // Fetch data
    TimeSlotService service = new TimeSlotService();
    List<TimeSlot> slots = service.getProfessorPastSlots(professor.getUserId());
    table.getItems().setAll(slots);

    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table.setPrefWidth(800);

    return table;
    }

    // helper method to make table for all upcoming slots
    private static TableView<TimeSlot> buildUpcomingSlotsView(Professor professor) {
    TableView<TimeSlot> table = new TableView<>();

    TableColumn<TimeSlot, Integer> idCol = new TableColumn<>("Slot ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("slotID")); // matches getSlotID()

    TableColumn<TimeSlot, String> dateCol = new TableColumn<>("Date");
    dateCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getSlotDate().toString()));

    TableColumn<TimeSlot, String> startCol = new TableColumn<>("Start");
    startCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getStartTime().toString()));

    TableColumn<TimeSlot, String> endCol = new TableColumn<>("End");
    endCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getEndTime().toString()));

    TableColumn<TimeSlot, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getStatus().toString()));

    TableColumn<TimeSlot, TimeSlotStatus> statusActionCol = new TableColumn<>("Change Status");
    statusActionCol.setCellFactory(col -> new TableCell<>() {
    private final ComboBox<TimeSlotStatus> combo = new ComboBox<>();

    {
        combo.getItems().addAll(TimeSlotStatus.CANCELLED,
                                TimeSlotStatus.FROZEN,
                            TimeSlotStatus.FREE);

        combo.setOnAction(e -> {
            TimeSlot slot = getTableView().getItems().get(getIndex());
            TimeSlotStatus selected = combo.getValue();

            TimeSlotService service = new TimeSlotService();
            boolean success = service.updateSlotStatus(slot.getSlotID(), selected);

            if (success) {
                slot.setStatus(selected);
                getTableView().refresh();
            }
        });
    }

    @Override
    protected void updateItem(TimeSlotStatus status, boolean empty) {
        super.updateItem(status, empty);
        if (empty) {
            setGraphic(null);
        } else {
            combo.setValue(status);
            setGraphic(combo);
        }
        }
    });

    TableColumn<TimeSlot, Void> blockCol = new TableColumn<>("Block/Unblock");
    blockCol.setCellFactory(col -> new TableCell<>() {
    private final Button blockBtn = new Button("Block");
    private final Button unblockBtn = new Button("Unblock");

    {
        blockBtn.setOnAction(e -> {
            TimeSlot slot = getTableView().getItems().get(getIndex());
            TimeSlotService service = new TimeSlotService();
            if (service.blockSlot(slot.getSlotID())) {
                slot.setStatus(TimeSlotStatus.LOCKED);
                slot.setIsManuallyBlockedByProf(true);
                getTableView().refresh();
            }
        });

        unblockBtn.setOnAction(e -> {
            TimeSlot slot = getTableView().getItems().get(getIndex());
            TimeSlotService service = new TimeSlotService();
            if (service.unblockSlot(slot.getSlotID())) {
                slot.setStatus(TimeSlotStatus.FREE);
                slot.setIsManuallyBlockedByProf(false);
                getTableView().refresh();
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
            if (slot.getIsManuallyBlockedByProf()) {
                setGraphic(unblockBtn);
            } else {
                setGraphic(blockBtn);
            }
        }
        }
    });



    table.getColumns().addAll(idCol, dateCol, startCol, endCol, statusCol, statusActionCol, blockCol);

    // Fetch data
    TimeSlotService service = new TimeSlotService();
    List<TimeSlot> slots = service.getProfessorUpcomingSlots(professor.getUserId());
    table.getItems().setAll(slots);

    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table.setPrefWidth(800);

    return table;
    }

    // helper method to build waitlist management view
    private static VBox buildWaitlistManagementView(Professor professor) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));

        Label title = new Label("Waitlist Management");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // Slot selector
        ComboBox<TimeSlot> slotSelector = new ComboBox<>();
        slotSelector.setPromptText("Select Slot to View Waitlist");

        slotSelector.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TimeSlot slot, boolean empty) {
                super.updateItem(slot, empty);
                if (empty || slot == null) {
                    setText(null);
                } else {
                    setText("Slot " + slot.getSlotID() + " — " + slot.getSlotDate() + " " + slot.getStartTime() + " to " + slot.getEndTime());
                }
            }
        });

        slotSelector.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TimeSlot slot) {
                if (slot == null) return "";
                return "Slot " + slot.getSlotID() + " — " + slot.getSlotDate() + " " + slot.getStartTime() + " to " + slot.getEndTime();
            }

            @Override
            public TimeSlot fromString(String string) {
                return null;
            }
        });

        // Populate slot selector with professor's slots
        TimeSlotService timeSlotService = new TimeSlotService();
        List<TimeSlot> professorSlots = timeSlotService.getProfessorUpcomingSlots(professor.getUserId());
        slotSelector.getItems().addAll(professorSlots);

        // Waitlist table
        TableView<WaitlistEntry> waitlistTable = new TableView<>();

        TableColumn<WaitlistEntry, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStudent().getFirstName() + " " +
                                   cellData.getValue().getStudent().getLastName()));

        TableColumn<WaitlistEntry, Integer> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priorityScore"));

        TableColumn<WaitlistEntry, String> joinedCol = new TableColumn<>("Joined At");
        joinedCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getJoinedAt().toString()));

        TableColumn<WaitlistEntry, Void> removeCol = new TableColumn<>("Remove");
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");

            {
                removeBtn.setOnAction(e -> {
                    WaitlistEntry entry = getTableView().getItems().get(getIndex());
                    WaitlistService waitlistService = new WaitlistService();
                    boolean success = waitlistService.removeFromWaitlist(entry.getWaitlistId());
                    if (success) {
                        getTableView().getItems().remove(entry);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Student removed from waitlist");
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to remove from waitlist");
                        alert.showAndWait();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        waitlistTable.getColumns().addAll(studentCol, priorityCol, joinedCol, removeCol);

        // Slot selector action
        slotSelector.setOnAction(e -> {
            TimeSlot selectedSlot = slotSelector.getValue();
            if (selectedSlot != null) {
                WaitlistService waitlistService = new WaitlistService();
                List<WaitlistEntry> waitlist = waitlistService.getWaitlistBySlot(selectedSlot.getSlotID());
                waitlistTable.getItems().setAll(waitlist);
            } else {
                waitlistTable.getItems().clear();
            }
        });

        container.getChildren().addAll(title, slotSelector, waitlistTable);
        return container;
    }

}


