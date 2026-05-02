package dao;

import enums.AppointmentReason;
import enums.AppointmentStatus;
import model.Appointment;
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
                   ad.rejection_reason, ad.created_at, ad.rescheduled_from
            FROM student_appointment sa
            JOIN appointment_details ad ON sa.appointment_id = ad.appointment_id
            WHERE sa.student_id = ?
            ORDER BY ad.created_at DESC
        """;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

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

            // 3. update timeslot current_bookings and status
            String updateSlot = """
                UPDATE timeslot
                SET current_bookings = current_bookings + 1,
                    status = CASE
                        WHEN current_bookings + 1 >= max_capacity THEN 'LOCKED'
                        ELSE 'PARTIALLY_BOOKED'
                    END
                WHERE slot_id = ?
            """;
            PreparedStatement slotStmt = conn.prepareStatement(updateSlot);
            slotStmt.setInt(1, slotId);
            slotStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return false;
        }
    }

    // cancel an appointment — only PENDING or APPROVED
    public boolean cancelAppointment(int appointmentId, int slotId) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // 1. update appointment status to CANCELLED
            String updateAppt = """
                UPDATE appointment_details
                SET status = 'CANCELLED'
                WHERE appointment_id = ?
                AND status IN ('PENDING', 'APPROVED')
            """;
            PreparedStatement apptStmt = conn.prepareStatement(updateAppt);
            apptStmt.setInt(1, appointmentId);
            int rows = apptStmt.executeUpdate();

            // if no rows updated it means status wasnt PENDING or APPROVED
            if (rows == 0) {
                conn.rollback();
                return false;
            }

            // 2. update timeslot current_bookings and status
            String updateSlot = """
                UPDATE timeslot
                SET current_bookings = current_bookings - 1,
                    status = CASE
                        WHEN current_bookings - 1 = 0 THEN 'FREE'
                        ELSE 'PARTIALLY_BOOKED'
                    END
                WHERE slot_id = ?
            """;
            PreparedStatement slotStmt = conn.prepareStatement(updateSlot);
            slotStmt.setInt(1, slotId);
            slotStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
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
                 "AND t.status NOT IN ('CANCELLED', 'LOCKED') " +
                 "AND t.slot_date >= CURDATE()";

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

    // update appointment status
    public boolean updateAppointmentStatus(int appointmentId, AppointmentStatus newStatus) {
    String sql = "UPDATE appointment_details SET status = ? WHERE appointment_id = ?";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, newStatus.name()); // store enum as string
        stmt.setInt(2, appointmentId);

        int rowsUpdated = stmt.executeUpdate();
        return rowsUpdated > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
    }


}