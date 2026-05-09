package view;

import java.util.List;

import enums.AppointmentStatus;
import enums.TimeSlotStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
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
        Button generateBtn  = new Button("Generate Slots");
        Button logoutBtn    = new Button("Logout");

        for (Button btn : new Button[]{
                pendingBtn, waitlistBtn, pastBtn, upcomingBtn,generateBtn, logoutBtn}) {
            btn.setPrefWidth(180);
            btn.setPrefHeight(35);
            btn.setFont(Font.font("Arial", 13));
        }

        VBox sidebar = new VBox(10,
            pendingBtn, waitlistBtn, pastBtn, upcomingBtn,generateBtn, logoutBtn);
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
        generateBtn.setOnAction(e -> 
            contentArea.getChildren().setAll(buildGenerateSlotsView(professor)));
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
                if (selected != AppointmentStatus.PENDING) {
                    getTableView().getItems().remove(appt);
                }
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

    // view for the generate slot button

    private static VBox buildGenerateSlotsView(Professor professor) {
    VBox layout = new VBox(15);
    layout.setPadding(new Insets(20));
    layout.setStyle("-fx-background-color: white;");

    // 1. Header Title
    Label header = new Label("Slot Management");
    header.setFont(Font.font("Arial", FontWeight.BOLD, 20));

    // 2. Manual Insert Button (Placed at the top of the workspace)
    Button manualInsertBtn = new Button("+ Manually Insert Custom Slot");
    manualInsertBtn.setStyle("-fx-background-color: #197c6f; -fx-text-fill: white; -fx-font-weight: bold;");
    manualInsertBtn.setPrefWidth(250);
    manualInsertBtn.setPrefHeight(40);
    
    // Action for Manual Insert
    manualInsertBtn.setOnAction(e -> showManualInsertDialog(professor));

    Separator separator = new Separator();

    // 3. Template List Area
    VBox templateList = new VBox(10);
    service.SlotGeneratorService genService = new service.SlotGeneratorService();
    List<model.SlotTemplate> templates = genService.getAllTemplates();

    for (model.SlotTemplate temp : templates) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #addfd5; -fx-border-radius: 5;");

        // Format: MONDAY | 10:30:00 - 11:00:00
        Label info = new Label(String.format("%-10s | %s - %s", 
            temp.getDay(), temp.getStartTime(), temp.getEndTime()));
        info.setFont(Font.font("Monospaced", 13)); 
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button genBtn = new Button("Generate");
        genBtn.setStyle("-fx-cursor: hand;");
        
        genBtn.setOnAction(e -> {
            // This calls the helper method we discussed to check duplicates
            handleTemplateGeneration(professor, temp);
        });

        row.getChildren().addAll(info, spacer, genBtn);
        templateList.getChildren().add(row);
    }

    // Wrap the list in a ScrollPane in case there are many slots
    ScrollPane scrollPane = new ScrollPane(templateList);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefHeight(400);
    scrollPane.setStyle("-fx-background-color:transparent;");

    // Add all components to the main layout
    layout.getChildren().addAll(header, manualInsertBtn, separator, scrollPane);
    
    return layout;
    }

    // helper method for generate slot view

    private static void handleTemplateGeneration(Professor professor, model.SlotTemplate temp) {
    service.SlotGeneratorService genService = new service.SlotGeneratorService();
    dao.TimeSlotDAO slotDAO = new dao.TimeSlotDAO(); // Ensure this matches your DAO class name
    
    // 1. Convert "MONDAY" string to DayOfWeek and find the upcoming date
    java.time.DayOfWeek dow = java.time.DayOfWeek.valueOf(temp.getDay().toUpperCase());
    java.time.LocalDate targetDate = java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(dow));
    
    // 2. Parse the start time from the template
    java.time.LocalTime start = java.time.LocalTime.parse(temp.getStartTime());
    java.time.LocalTime end = java.time.LocalTime.parse(temp.getEndTime());

    // 3. DUPLICATE CHECK: Call the method we added to your DAO earlier
    if (slotDAO.isSlotDuplicate(professor.getUserId(), targetDate, start)) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Duplicate Slot");
        alert.setHeaderText("Scheduling Conflict");
        alert.setContentText("You already have a slot at " + start + " on " + targetDate + ".");
        alert.show();
    } else {
        // 4. If no duplicate, create the TimeSlot and save to AWS MySQL
        model.TimeSlot newSlot = new model.TimeSlot(targetDate, professor.getUserId(), start, end);
        
        boolean success = slotDAO.addSlot(newSlot);
        
        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Successfully generated slot for " + targetDate);
            alert.show();
        } else {
            new Alert(Alert.AlertType.ERROR, "Failed to save slot to database.").show();
        }
    }
    }

    // for the manual slot insert button

    private static void showManualInsertDialog(Professor professor) {
    // 1. Create the Dialog container
    Dialog<model.TimeSlot> dialog = new Dialog<>();
    dialog.setTitle("Manual Slot Entry");
    dialog.setHeaderText("Enter details for a custom time slot");

    // 2. Set up the "Save" and "Cancel" buttons
    ButtonType saveButtonType = new ButtonType("Save Slot", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

    // 3. Create the input fields
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    DatePicker datePicker = new DatePicker(java.time.LocalDate.now());
    TextField startTimeField = new TextField("09:00"); 
    TextField endTimeField = new TextField("09:30");

    grid.add(new Label("Select Date:"), 0, 0);
    grid.add(datePicker, 1, 0);
    grid.add(new Label("Start Time (HH:mm):"), 0, 1);
    grid.add(startTimeField, 1, 1);
    grid.add(new Label("End Time (HH:mm):"), 0, 2);
    grid.add(endTimeField, 1, 2);

    dialog.getDialogPane().setContent(grid);

    // 4. Convert the dialog result into a TimeSlot object when Save is clicked
    dialog.setResultConverter(dialogButton -> {
        if (dialogButton == saveButtonType) {
            try {
                return new model.TimeSlot(
                    datePicker.getValue(),
                    professor.getUserId(),
                    java.time.LocalTime.parse(startTimeField.getText()),
                    java.time.LocalTime.parse(endTimeField.getText())
                );
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid time format. Please use HH:mm").show();
                return null;
            }
        }
        return null;
    });

    // 5. Handle the result (Duplicate Check + Save)
    dialog.showAndWait().ifPresent(newSlot -> {
        dao.TimeSlotDAO slotDAO = new dao.TimeSlotDAO();
        
        if (slotDAO.isSlotDuplicate(newSlot.getProfessorID(), newSlot.getSlotDate(), newSlot.getStartTime())) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Conflict: A slot already exists at this time.");
            alert.show();
        } else {
            boolean success = slotDAO.addSlot(newSlot);
            if (success) {
                new Alert(Alert.AlertType.INFORMATION, "Custom slot added successfully!").show();
            }
        }
        });
    }

}


