package dao;

import enums.AppointmentReason;
import enums.AppointmentStatus;
import model.Appointment;
import model.Student;
import model.TimeSlot;
import model.WaitlistEntry;
import service.TimeSlotService;
import service.WaitlistService;
import priority.PriorityCalculator;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    // get all appointments for a student
    public List<Appointment> getAppointmentsByStudent(int studentId) {
        List<Appointment> list = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        if (conn == null) return list;

        String query = """
            SELECT ad.appointment_id, sa.student_id, sa.slot_id,
                ad.status, ad.reason, ad.note,
                ad.rejection_reason, ad.created_at, ad.rescheduled_from,
                ud.first_name, ud.last_name,
                t.slot_date, t.start_time, t.end_time
            FROM student_appointment sa
            JOIN appointment_details ad ON sa.appointment_id = ad.appointment_id
            JOIN timeslot t ON sa.slot_id = t.slot_id
            JOIN user_details ud ON t.professor_id = ud.user_id
            WHERE sa.student_id = ?
            ORDER BY ad.created_at DESC
        """;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            // REPLACE WITH THIS — adds the extra fields:
            while (rs.next()) {
                Appointment a = new Appointment(
                    rs.getInt("appointment_id"),
                    rs.getInt("student_id"),
                    rs.getInt("slot_id"),
                    AppointmentStatus.valueOf(rs.getString("status")),
                    AppointmentReason.valueOf(rs.getString("reason")),
                    rs.getString("note"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                );
                a.setRejectionReason(rs.getString("rejection_reason"));
                Integer reschedFrom = rs.getObject("rescheduled_from") != null
                    ? rs.getInt("rescheduled_from") : null;
                a.setRescheduledFrom(reschedFrom);

                // SET PROFESSOR NAME AND SLOT DETAILS
                a.setProfessorName(
                    rs.getString("first_name") + " " + rs.getString("last_name"));
                a.setSlotDate(rs.getDate("slot_date").toLocalDate());
                a.setSlotStartTime(rs.getTime("start_time").toLocalTime());
                a.setSlotEndTime(rs.getTime("end_time").toLocalTime());

                list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // get all FREE and PARTIALLY_BOOKED slots with professor name
    public List<Object[]> getAvailableSlots() {
        List<Object[]> list = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        if (conn == null) return list;

        String query = """
            SELECT t.slot_id, t.professor_id,
                   ud.first_name, ud.last_name,
                   t.slot_date, t.start_time, t.end_time,
                   t.max_capacity, t.current_bookings
            FROM timeslot t
            JOIN user_details ud ON t.professor_id = ud.user_id
            WHERE t.status IN ('FREE', 'PARTIALLY_BOOKED')
            AND t.is_manually_blocked_by_prof = FALSE
            AND t.slot_date >= CURDATE()
            ORDER BY t.slot_date, t.start_time
        """;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] row = new Object[]{
                    rs.getInt("slot_id"),
                    rs.getInt("professor_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getDate("slot_date").toLocalDate(),
                    rs.getTime("start_time").toLocalTime(),
                    rs.getTime("end_time").toLocalTime(),
                    rs.getInt("max_capacity") - rs.getInt("current_bookings")
                };
                list.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // book an appointment
    public boolean bookAppointment(int studentId, int slotId,
                                    AppointmentReason reason) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // 1. insert into student_appointment
            String insertAppt = """
                INSERT INTO student_appointment (student_id, slot_id)
                VALUES (?, ?)
            """;
            PreparedStatement apptStmt = conn.prepareStatement(
                insertAppt, Statement.RETURN_GENERATED_KEYS);
            apptStmt.setInt(1, studentId);
            apptStmt.setInt(2, slotId);
            apptStmt.executeUpdate();

            ResultSet keys = apptStmt.getGeneratedKeys();
            keys.next();
            int appointmentId = keys.getInt(1);

            // 2. insert into appointment_details
            String insertDetails = """
                INSERT INTO appointment_details
                (appointment_id, status, reason, created_at)
                VALUES (?, 'PENDING', ?, ?)
            """;
            PreparedStatement detailStmt = conn.prepareStatement(insertDetails);
            detailStmt.setInt(1, appointmentId);
            detailStmt.setString(2, reason.name());
            detailStmt.setTimestamp(3,
                Timestamp.valueOf(LocalDateTime.now()));
            detailStmt.executeUpdate();

            // booking remains PENDING; count and slot locking happen when a professor approves the appointment
            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return false;
        }
    }

    // cancel an appointment — only pending, approved, or waitlisted
    public boolean cancelAppointment(int appointmentId, int slotId) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.out.println("Failed to get database connection");
            return false;
        }

        try {
            conn.setAutoCommit(false);

            // Fetch appointment details using the same connection
            String selectAppt = """
                SELECT sa.appointment_id, sa.student_id, sa.slot_id, ad.status
                FROM student_appointment sa
                JOIN appointment_details ad ON sa.appointment_id = ad.appointment_id
                WHERE sa.appointment_id = ?
            """;
            PreparedStatement selectStmt = conn.prepareStatement(selectAppt);
            selectStmt.setInt(1, appointmentId);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Appointment not found: " + appointmentId);
                conn.rollback();
                return false;
            }

            String statusStr = rs.getString("status");
            AppointmentStatus originalStatus = AppointmentStatus.valueOf(statusStr);
            System.out.println("Current appointment status: " + originalStatus);

            // Check if appointment can be cancelled
            if (!statusStr.equals("PENDING") && !statusStr.equals("APPROVED") && !statusStr.equals("WAITLISTED")) {
                System.out.println("Appointment cannot be cancelled - status is: " + statusStr);
                conn.rollback();
                return false;
            }

            // Update appointment to CANCELLED
            String updateAppt = """
                UPDATE appointment_details
                SET status = 'CANCELLED'
                WHERE appointment_id = ?
            """;
            PreparedStatement updateStmt = conn.prepareStatement(updateAppt);
            updateStmt.setInt(1, appointmentId);
            int rows = updateStmt.executeUpdate();
            System.out.println("Rows updated: " + rows);

            if (rows == 0) {
                System.out.println("Failed to update appointment status");
                conn.rollback();
                return false;
            }

            // If appointment was APPROVED, decrement slot booking count
            if (originalStatus == AppointmentStatus.APPROVED) {
                if (!adjustSlotBookingCount(conn, slotId, -1)) {
                    System.out.println("Failed to adjust slot booking count");
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            System.out.println("Appointment cancelled successfully");
            return true;

        } catch (Exception e) {
            System.out.println("Error cancelling appointment: " + e.getMessage());
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }

    // get all the pending appointments for a professor
    public List<Appointment> getPendingAppointmentsForProfessor(int professorId) {
    List<Appointment> appointments = new ArrayList<>();

    String sql = "SELECT sa.appointment_id, sa.student_id, sa.slot_id, " +
                 "ad.status, ad.reason, ad.note, ad.rejection_reason, " +
                 "ad.created_at, ad.rescheduled_from " +
                 "FROM student_appointment sa " +
                 "JOIN appointment_details ad ON sa.appointment_id = ad.appointment_id " +
                 "JOIN timeslot t ON sa.slot_id = t.slot_id " +
                 "WHERE t.professor_id = ? " +
                 "AND ad.status = 'PENDING' " +
                "AND t.status NOT IN ('CANCELLED', 'LOCKED') ";
                //  +
                //  "AND t.slot_date >= CURDATE()";    temporarily allow past pending appts to show for testing

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setInt(1, professorId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            Appointment appt = new Appointment(
                rs.getInt("appointment_id"),
                rs.getInt("student_id"),
                rs.getInt("slot_id"),
                AppointmentStatus.valueOf(rs.getString("status").toUpperCase()),
                AppointmentReason.valueOf(rs.getString("reason").toUpperCase()),
                rs.getString("note"),
                rs.getTimestamp("created_at").toLocalDateTime()
            );

            appt.setRejectionReason(rs.getString("rejection_reason"));
            appt.setRescheduledFrom((Integer) rs.getObject("rescheduled_from"));

            appointments.add(appt);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return appointments;
}

    // update appointment status and adjust slot booking counts on approval/cancellation
    public boolean updateAppointmentStatus(int appointmentId, AppointmentStatus newStatus) {
        Appointment existing = getAppointmentById(appointmentId);
        if (existing == null) {
            return false;
        }

        // FETCH ALL DATA BEFORE STARTING TRANSACTION to avoid connection issues
        Student student = null;
        TimeSlot slot = null;
        
        if (newStatus == AppointmentStatus.WAITLISTED || existing.getStatus() == AppointmentStatus.WAITLISTED) {
            student = getUserById(existing.getStudentId());
            TimeSlotDAO timeSlotDAO = new TimeSlotDAO();
            slot = timeSlotDAO.getSlotById(existing.getSlotId());
        }

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        try {
            conn.setAutoCommit(false);

            if (existing.getStatus() != AppointmentStatus.APPROVED
                    && newStatus == AppointmentStatus.APPROVED) {
                if (!adjustSlotBookingCount(conn, existing.getSlotId(), +1)) {
                    conn.rollback();
                    return false;
                }
            } else if (existing.getStatus() == AppointmentStatus.APPROVED
                    && newStatus != AppointmentStatus.APPROVED) {
                if (!adjustSlotBookingCount(conn, existing.getSlotId(), -1)) {
                    conn.rollback();
                    return false;
                }
            }

            // Handle WAITLISTED transitions
            if (existing.getStatus() != AppointmentStatus.WAITLISTED
                    && newStatus == AppointmentStatus.WAITLISTED) {
                // Add to waitlist tables when transitioning TO WAITLISTED
                try {
                    if (student != null && slot != null) {
                        int priorityScore = PriorityCalculator.calculatePriorityScore(existing, student);
                        
                        // Insert into waitlisted_student
                        String insertStudentQuery = "INSERT INTO waitlisted_student(student_id) VALUES (?)";
                        PreparedStatement ps1 = conn.prepareStatement(insertStudentQuery, new String[]{"waitlist_id"});
                        ps1.setInt(1, student.getUserId());
                        int affectedRows = ps1.executeUpdate();
                        if (affectedRows == 0) {
                            conn.rollback();
                            return false;
                        }
                        ResultSet generatedKeys = ps1.getGeneratedKeys();
                        int waitlistId;
                        if (generatedKeys.next()) {
                            waitlistId = generatedKeys.getInt(1);
                        } else {
                            conn.rollback();
                            return false;
                        }
                        
                        // Insert into waitlist_details
                        String insertDetailsQuery = "INSERT INTO waitlist_details (waitlist_id, priority_score, joined_at, slot_id) VALUES (?, ?, ?, ?)";
                        PreparedStatement ps2 = conn.prepareStatement(insertDetailsQuery);
                        ps2.setInt(1, waitlistId);
                        ps2.setInt(2, priorityScore);
                        ps2.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
                        ps2.setInt(4, slot.getSlotID());
                        int rowsInserted = ps2.executeUpdate();
                        if (rowsInserted == 0) {
                            conn.rollback();
                            return false;
                        }
                        
                        System.out.println("Student added to waitlist for appointment " + appointmentId);
                    } else {
                        System.out.println("Failed to fetch student or slot for waitlist");
                        conn.rollback();
                        return false;
                    }
                } catch (Exception ex) {
                    System.out.println("Error adding to waitlist: " + ex.getMessage());
                    ex.printStackTrace();
                    conn.rollback();
                    return false;
                }
            } else if (existing.getStatus() == AppointmentStatus.WAITLISTED
                    && newStatus != AppointmentStatus.WAITLISTED) {
                // Remove from waitlist when transitioning FROM WAITLISTED
                try {
                    // Find the waitlist_id
                    String selectQuery = "SELECT ws.waitlist_id FROM waitlisted_student ws JOIN waitlist_details wd ON ws.waitlist_id = wd.waitlist_id WHERE ws.student_id = ? AND wd.slot_id = ?";
                    PreparedStatement psSelect = conn.prepareStatement(selectQuery);
                    psSelect.setInt(1, existing.getStudentId());
                    psSelect.setInt(2, existing.getSlotId());
                    ResultSet rs = psSelect.executeQuery();
                    if (rs.next()) {
                        int waitlistId = rs.getInt("waitlist_id");
                        
                        // Delete from waitlist_details
                        String deleteDetailsQuery = "DELETE FROM waitlist_details WHERE waitlist_id = ?";
                        PreparedStatement ps1 = conn.prepareStatement(deleteDetailsQuery);
                        ps1.setInt(1, waitlistId);
                        ps1.executeUpdate();
                        
                        // Delete from waitlisted_student
                        String deleteStudentQuery = "DELETE FROM waitlisted_student WHERE waitlist_id = ?";
                        PreparedStatement ps2 = conn.prepareStatement(deleteStudentQuery);
                        ps2.setInt(1, waitlistId);
                        ps2.executeUpdate();
                    }
                    
                    System.out.println("Student removed from waitlist for appointment " + appointmentId);
                } catch (Exception ex) {
                    System.out.println("Error removing from waitlist: " + ex.getMessage());
                    ex.printStackTrace();
                    conn.rollback();
                    return false;
                }
            }

            String sql = "UPDATE appointment_details SET status = ? WHERE appointment_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newStatus.name());
                stmt.setInt(2, appointmentId);
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            System.out.println("Appointment " + appointmentId + " status updated to " + newStatus);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Helper method to get student by ID
    private Student getUserById(int userId) {
        String sql = """
            SELECT ue.user_id, ue.email, ud.password_hash, ud.first_name, ud.last_name, ud.phone_number, s.year
            FROM user_email ue
            JOIN user_details ud ON ue.user_id = ud.user_id
            JOIN student s ON ue.user_id = s.student_id
            WHERE ue.user_id = ?
        """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Student(
                    rs.getInt("user_id"),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("phone_number"),
                    rs.getInt("year")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean adjustSlotBookingCount(Connection conn, int slotId, int delta) throws SQLException {
        if (delta == 1) {
            String sql = """
                UPDATE timeslot
                SET current_bookings = current_bookings + 1,
                    status = CASE
                        WHEN current_bookings + 1 >= max_capacity THEN 'LOCKED'
                        ELSE 'PARTIALLY_BOOKED'
                    END
                WHERE slot_id = ?
                  AND current_bookings < max_capacity
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, slotId);
                return stmt.executeUpdate() > 0;
            }
        } else if (delta == -1) {
            String sql = """
                UPDATE timeslot
                SET current_bookings = current_bookings - 1,
                    status = CASE
                        WHEN current_bookings - 1 = 0 THEN 'FREE'
                        ELSE 'PARTIALLY_BOOKED'
                    END
                WHERE slot_id = ?
                  AND current_bookings > 0
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, slotId);
                return stmt.executeUpdate() > 0;
            }
        }
        return false;
    }

    // get appointment by id
    public Appointment getAppointmentById(int appointmentId) {
        String sql = "SELECT sa.appointment_id, sa.student_id, sa.slot_id, " +
                     "ad.status, ad.reason, ad.note, ad.rejection_reason, " +
                     "ad.created_at, ad.rescheduled_from " +
                     "FROM student_appointment sa " +
                     "JOIN appointment_details ad ON sa.appointment_id = ad.appointment_id " +
                     "WHERE sa.appointment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, appointmentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Appointment appt = new Appointment(
                    rs.getInt("appointment_id"),
                    rs.getInt("student_id"),
                    rs.getInt("slot_id"),
                    AppointmentStatus.valueOf(rs.getString("status").toUpperCase()),
                    AppointmentReason.valueOf(rs.getString("reason").toUpperCase()),
                    rs.getString("note"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                );
                appt.setRejectionReason(rs.getString("rejection_reason"));
                appt.setRescheduledFrom((Integer) rs.getObject("rescheduled_from"));
                return appt;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // get appointment by student and slot
    public Appointment getAppointmentByStudentAndSlot(int studentId, int slotId) {
        String sql = "SELECT sa.appointment_id, sa.student_id, sa.slot_id, " +
                     "ad.status, ad.reason, ad.note, ad.rejection_reason, " +
                     "ad.created_at, ad.rescheduled_from " +
                     "FROM student_appointment sa " +
                     "JOIN appointment_details ad ON sa.appointment_id = ad.appointment_id " +
                     "WHERE sa.student_id = ? AND sa.slot_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, slotId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Appointment appt = new Appointment(
                    rs.getInt("appointment_id"),
                    rs.getInt("student_id"),
                    rs.getInt("slot_id"),
                    AppointmentStatus.valueOf(rs.getString("status").toUpperCase()),
                    AppointmentReason.valueOf(rs.getString("reason").toUpperCase()),
                    rs.getString("note"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                );
                appt.setRejectionReason(rs.getString("rejection_reason"));
                appt.setRescheduledFrom((Integer) rs.getObject("rescheduled_from"));
                return appt;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}