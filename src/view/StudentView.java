package view;

import enums.AppointmentReason;
import enums.AppointmentStatus;
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
import model.Student;
import model.WaitlistEntry;
import service.AppointmentService;
import service.WaitlistService;
import java.util.List;

public class StudentView {

    private static AppointmentService appointmentService
        = new AppointmentService();

    public static Scene getScene(Student student) {

        // ── TOP BAR ────────────────────────────────────
        Label appTitle = new Label("Appointment System");
        appTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Label welcomeLabel = new Label(
            "Welcome, " + student.getFirstName()
            + " " + student.getLastName());
        welcomeLabel.setFont(Font.font("Arial", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, appTitle, spacer, welcomeLabel);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #f0f0f0;");

        // ── SIDEBAR ────────────────────────────────────
        Button viewApptBtn  = new Button("My Appointments");
        Button bookApptBtn  = new Button("Book Appointment");
        Button waitlistBtn  = new Button("My Waitlist");
        Button cancelApptBtn = new Button("Cancel Appointment");
        Button logoutBtn    = new Button("Logout");

        // style all sidebar buttons same size
        for (Button btn : new Button[]{
                viewApptBtn, bookApptBtn, waitlistBtn, cancelApptBtn, logoutBtn}) {
            btn.setPrefWidth(160);
            btn.setPrefHeight(35);
            btn.setFont(Font.font("Arial", 13));
        }

        VBox sidebar = new VBox(10,
            viewApptBtn, bookApptBtn, waitlistBtn, cancelApptBtn, logoutBtn);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(180);
        sidebar.setStyle("-fx-background-color: #e8e8e8;");

        // ── MAIN CONTENT AREA ──────────────────────────
        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));

        // load default view
        contentArea.getChildren().setAll(
            buildViewAppointments(student));

        // ── SIDEBAR BUTTON ACTIONS ─────────────────────
        viewApptBtn.setOnAction(e ->
            contentArea.getChildren().setAll(
                buildViewAppointments(student)));

        bookApptBtn.setOnAction(e ->
            contentArea.getChildren().setAll(
                buildBookAppointment(student)));

        waitlistBtn.setOnAction(e ->
            contentArea.getChildren().setAll(
                buildViewWaitlist(student)));

        cancelApptBtn.setOnAction(e ->
            contentArea.getChildren().setAll(
                buildCancelAppointment(student)));

        logoutBtn.setOnAction(e ->
            Main.showLoginScreen());

        // ── ROOT LAYOUT ────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        return new Scene(root, 800, 600);
    }

    // ── VIEW MY APPOINTMENTS ───────────────────────────
    private static VBox buildViewAppointments(Student student) {

        Label title = new Label("My Appointments");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TableView<Appointment> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Appt ID
        TableColumn<Appointment, Integer> idCol = new TableColumn<>("Appt ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));

        // Slot ID
        TableColumn<Appointment, Integer> slotCol = new TableColumn<>("Slot ID");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotId"));

        // Professor Name  ← NEW
        TableColumn<Appointment, String> profCol = new TableColumn<>("Professor");
        profCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getProfessorName()));

        // Slot Date  ← NEW
        TableColumn<Appointment, String> slotDateCol = new TableColumn<>("Date");
        slotDateCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSlotDate() != null
                    ? cellData.getValue().getSlotDate().toString() : ""));

        // Slot Time  ← NEW
        TableColumn<Appointment, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> {
            Appointment a = cellData.getValue();
            String time = (a.getSlotStartTime() != null && a.getSlotEndTime() != null)
                ? a.getSlotStartTime() + " - " + a.getSlotEndTime()
                : "";
            return new javafx.beans.property.SimpleStringProperty(time);
        });

        // Reason
        TableColumn<Appointment, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));

        // Status
        TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Booked At
        TableColumn<Appointment, String> bookedCol = new TableColumn<>("Booked At");
        bookedCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt().toString()));

        table.getColumns().addAll(
            idCol, slotCol, profCol, slotDateCol, timeCol,
            reasonCol, statusCol, bookedCol);

        List<Appointment> appointments =
            appointmentService.getStudentAppointments(student.getUserId());
        table.getItems().addAll(appointments);

        Label countLabel = new Label(
            "Total: " + appointments.size() + " appointment(s)");
        countLabel.setFont(Font.font("Arial", 13));

        VBox layout = new VBox(10, title, countLabel, table);
        layout.setPadding(new Insets(10));
        return layout;
    }
    // ── BOOK APPOINTMENT ──────────────────────────────
    private static VBox buildViewWaitlist(Student student) {
        Label title = new Label("My Waitlist");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TableView<WaitlistEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<WaitlistEntry, Integer> idCol = new TableColumn<>("Waitlist ID");
        idCol.setCellValueFactory(
            new PropertyValueFactory<>("waitlistId"));

        TableColumn<WaitlistEntry, Integer> slotCol = new TableColumn<>("Slot ID");
        slotCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getSlot().getSlotID()).asObject());

        TableColumn<WaitlistEntry, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStudent().getFirstName() + " " +
                cellData.getValue().getStudent().getLastName()));

        TableColumn<WaitlistEntry, Integer> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(
            new PropertyValueFactory<>("priorityScore"));

        TableColumn<WaitlistEntry, String> joinedCol = new TableColumn<>("Joined At");
        joinedCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getJoinedAt().toString()));

        table.getColumns().addAll(
            idCol, slotCol, studentCol, priorityCol, joinedCol);

        WaitlistService waitlistService = new WaitlistService();
        List<WaitlistEntry> waitlist =
            waitlistService.getWaitlistByStudent(student.getUserId());
        table.getItems().addAll(waitlist);

        VBox layout = new VBox(10, title, table);
        layout.setPadding(new Insets(10));
        return layout;
    }

    private static VBox buildBookAppointment(Student student) {

        Label title = new Label("Book an Appointment");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // reason dropdown
        Label reasonLabel = new Label("Select Reason:");
        reasonLabel.setFont(Font.font("Arial", 14));

        ComboBox<AppointmentReason> reasonBox =
            new ComboBox<>();
        reasonBox.getItems().addAll(AppointmentReason.values());
        reasonBox.setPromptText("Choose a reason");
        reasonBox.setPrefWidth(250);

        // search label 
        Label searchLabel = new Label("Search Professor:");
        searchLabel.setFont(Font.font("Arial", 14));

        TextField searchField = new TextField();
        searchField.setPromptText("Type professor name...");
        searchField.setPrefWidth(250);

        // feedback label
        Label feedbackLabel = new Label("");
        feedbackLabel.setFont(Font.font("Arial", 13));

        // table of available slots
        TableView<Object[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Object[], String> profCol =
            new TableColumn<>("Professor");
        profCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                (String) data.getValue()[2]));

        TableColumn<Object[], String> dateCol =
            new TableColumn<>("Date");
        dateCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[3].toString()));

        TableColumn<Object[], String> startCol =
            new TableColumn<>("Start");
        startCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[4].toString()));

        TableColumn<Object[], String> endCol =
            new TableColumn<>("End");
        endCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[5].toString()));

        TableColumn<Object[], String> spotsCol =
            new TableColumn<>("Spots Left");
        spotsCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue()[6].toString()));

        // book button column
        TableColumn<Object[], Void> actionCol =
            new TableColumn<>("Action");
        actionCol.setCellFactory(col ->
            new TableCell<>() {
                private final Button bookBtn = new Button("Book");
                {
                    bookBtn.setPrefWidth(70);
                    bookBtn.setOnAction(e -> {
                        Object[] row = getTableView()
                            .getItems().get(getIndex());
                        int slotId = (int) row[0];

                        AppointmentReason reason = reasonBox.getValue();

                        if (reason == null) {
                            feedbackLabel.setTextFill(Color.RED);
                            feedbackLabel.setText("Please select a reason first");
                            return;
                        }

                        boolean success =
                            appointmentService.bookAppointment(student.getUserId(), slotId, reason);

                        if (success) {
                            feedbackLabel.setTextFill(Color.GREEN);
                            feedbackLabel.setText("Appointment booked successfully!");
                            // reload and re-filter after booking
                            loadAndFilter(table, searchField.getText().trim());
                        } else {
                            feedbackLabel.setTextFill(Color.RED);
                            feedbackLabel.setText("Booking failed. Please try again.");
                        }
                    });
                }

                @Override
                protected void updateItem(Void item,boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : bookBtn);
                }
            });

        table.getColumns().addAll(profCol, dateCol, startCol,endCol, spotsCol, actionCol);

        // load all slots initially
        loadAndFilter(table, "");

        // ── ADD THIS: search listener ─────────────────────
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
            loadAndFilter(table, newVal.trim()));
        // ─────────────────────────────────────────────────

        HBox reasonRow = new HBox(10, reasonLabel, reasonBox);
        reasonRow.setAlignment(Pos.CENTER_LEFT);

        // ── ADD searchRow to layout ───────────────────────
        HBox searchRow = new HBox(10, searchLabel, searchField);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        VBox layout = new VBox(10,
            title, reasonRow, searchRow, feedbackLabel, table);
        layout.setPadding(new Insets(10));
        return layout;
    }
    // helper method
    private static void loadAndFilter(TableView<Object[]> table, String search) {
        List<Object[]> allSlots = appointmentService.getAvailableSlots();
        table.getItems().clear();

        for (Object[] row : allSlots) {
            String profName = (String) row[2];
            // if search is empty show all, else filter by professor name
            if (search.isEmpty() ||
                profName.toLowerCase().contains(search.toLowerCase())) {
                table.getItems().add(row);
            }
        }
    }

    // ── CANCEL APPOINTMENT ────────────────────────────
    private static VBox buildCancelAppointment(Student student) {

        Label title = new Label("Cancel an Appointment");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label info = new Label(
            "Only PENDING, APPROVED, and WAITLISTED appointments can be cancelled.");
        info.setFont(Font.font("Arial", 13));
        info.setTextFill(Color.GRAY);

        // feedback label
        Label feedbackLabel = new Label("");
        feedbackLabel.setFont(Font.font("Arial", 13));

        // table
        TableView<Appointment> table = new TableView<>();
        table.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Appointment, Integer> idCol =
            new TableColumn<>("Appt ID");
        idCol.setCellValueFactory(
            new PropertyValueFactory<>("appointmentId"));

        TableColumn<Appointment, Integer> slotCol =
            new TableColumn<>("Slot ID");
        slotCol.setCellValueFactory(
            new PropertyValueFactory<>("slotId"));

        TableColumn<Appointment, String> reasonCol =
            new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(
            new PropertyValueFactory<>("reason"));

        TableColumn<Appointment, String> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(
            new PropertyValueFactory<>("status"));

        // cancel button column
        TableColumn<Appointment, Void> actionCol =
            new TableColumn<>("Action");
        actionCol.setCellFactory(col ->
            new TableCell<>() {
                private final Button cancelBtn =
                    new Button("Cancel");
                {
                    cancelBtn.setPrefWidth(70);
                    cancelBtn.setStyle(
                        "-fx-text-fill: red;");
                    cancelBtn.setOnAction(e -> {
                        Appointment appt =
                            getTableView().getItems()
                                .get(getIndex());

                        boolean success =
                            appointmentService.cancelAppointment(
                                appt.getAppointmentId(),
                                appt.getSlotId());

                        Alert alert;
                        if (success) {
                            alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Success");
                            alert.setHeaderText("Appointment Cancelled");
                            alert.setContentText("Your appointment has been cancelled successfully.");
                            getTableView().getItems().remove(appt);
                        } else {
                            alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Cancellation Failed");
                            alert.setHeaderText("Unable to cancel appointment");
                            alert.setContentText("Failed to cancel the appointment. Please try again.");
                        }
                        alert.showAndWait();
                    });
                }

                @Override
                protected void updateItem(Void item,
                                           boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : cancelBtn);
                }
            });

        table.getColumns().addAll(
            idCol, slotCol, reasonCol, statusCol, actionCol);

        // load only PENDING and APPROVED appointments
        List<Appointment> all =
            appointmentService.getStudentAppointments(
                student.getUserId());

        for (Appointment a : all) {
            if (a.getStatus() == AppointmentStatus.PENDING
                || a.getStatus() == AppointmentStatus.APPROVED
                || a.getStatus() == AppointmentStatus.WAITLISTED) {
                table.getItems().add(a);
            }
        }

        if (table.getItems().isEmpty()) {
            Label empty = new Label(
                "No cancellable appointments found.");
            empty.setTextFill(Color.GRAY);
            VBox layout = new VBox(10,
                title, info, empty);
            layout.setPadding(new Insets(10));
            return layout;
        }

        VBox layout = new VBox(10,
            title, info, feedbackLabel, table);
        layout.setPadding(new Insets(10));
        return layout;
    }
}