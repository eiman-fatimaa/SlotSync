package dao;

import enums.TimeSlotStatus;
import model.Student;
import model.TimeSlot;
import model.WaitlistEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class WaitlistDAO {

    // Add student to waitlist
    public boolean addToWaitlist(WaitlistEntry entry) {

        String insertStudentQuery =
                "INSERT INTO waitlisted_student(student_id) VALUES (?)";

        String insertDetailsQuery =
                "INSERT INTO waitlist_details " +
                "(waitlist_id, priority_score, joined_at, slot_id) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            // STEP 1: add a Student to Waitlisted_Student to get waitlist_id
            PreparedStatement ps1 =
                    conn.prepareStatement(
                            insertStudentQuery,
                            Statement.RETURN_GENERATED_KEYS
                    );

            //set student_id parameter
            ps1.setInt(
                    1,
                    entry.getStudent().getUserId()
            );

            int affectedRows = ps1.executeUpdate();

            if (affectedRows == 0) {
                conn.rollback();
                return false;
            }

            // Get generated waitlist_id
            ResultSet generatedKeys =
                    ps1.getGeneratedKeys();

            int waitlistId;

            if (generatedKeys.next()) {

                waitlistId =
                        generatedKeys.getInt(1);

            } else {

                conn.rollback();
                return false;
            }

            // STEP 2
            PreparedStatement ps2 =
                    conn.prepareStatement(
                            insertDetailsQuery
                    );

            ps2.setInt(1, waitlistId);

            ps2.setInt(
                    2,
                    entry.getPriorityScore()
            );

            ps2.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            entry.getJoinedAt()
                    )
            );

            ps2.setInt(
                    4,
                    entry.getSlot().getSlotID()
            );

            int rowsInserted =
                    ps2.executeUpdate();

            if (rowsInserted > 0) {

                conn.commit();
                return true;

            } else {

                conn.rollback();
                return false;
            }

        } catch (SQLException e) {

            e.printStackTrace();
            return false;
        }
    }

    // Check if student already waitlisted
    public boolean isStudentWaitlisted(
            int studentId,
            int slotId) {

        String query =
                "SELECT * " +
                "FROM waitlisted_student ws " +
                "JOIN waitlist_details wd " +
                "ON ws.waitlist_id = wd.waitlist_id " +
                "WHERE ws.student_id = ? " +
                "AND wd.slot_id = ?";

        try (Connection conn =
                     DBConnection.getConnection();

             PreparedStatement ps =
                     conn.prepareStatement(query)) {

            ps.setInt(1, studentId);
            ps.setInt(2, slotId);

            ResultSet rs =
                    ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return false;
    }

    // Get all waitlisted students for a slot
    public List<WaitlistEntry>
    getWaitlistBySlot(int slotId) {

        List<WaitlistEntry> waitlist =
                new ArrayList<>();

        String query =
                "SELECT ws.waitlist_id, wd.priority_score, wd.joined_at, wd.slot_id, " +
                "ue.user_id, ue.email, ud.password_hash, ud.first_name, ud.last_name, " +
                "ud.phone_number, s.year, " +
                "t.slot_date, t.professor_id, t.start_time, t.end_time, t.status, " +
                "t.reserved_count, t.current_bookings, t.max_capacity, t.is_manually_blocked_by_prof " +
                "FROM waitlisted_student ws " +
                "JOIN waitlist_details wd ON ws.waitlist_id = wd.waitlist_id " +
                "JOIN user_email ue ON ws.student_id = ue.user_id " +
                "JOIN user_details ud ON ue.user_id = ud.user_id " +
                "JOIN student s ON ue.user_id = s.student_id " +
                "JOIN timeslot t ON wd.slot_id = t.slot_id " +
                "WHERE wd.slot_id = ? " +
                "ORDER BY wd.priority_score DESC, wd.joined_at ASC";

        try (Connection conn =
                     DBConnection.getConnection();

             PreparedStatement ps =
                     conn.prepareStatement(query)) {

            ps.setInt(1, slotId);

            ResultSet rs =
                    ps.executeQuery();

            while (rs.next()) {

                Student student = mapStudentFromResultSet(rs);
                TimeSlot slot = mapTimeSlotFromResultSet(rs);

                WaitlistEntry entry =
                        new WaitlistEntry(
                                rs.getInt("waitlist_id"),
                                student,
                                slot,
                                rs.getInt("priority_score"),
                                rs.getTimestamp("joined_at").toLocalDateTime()
                        );

                waitlist.add(entry);
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return waitlist;
    }

    // Get waitlist entries for a student
    public List<WaitlistEntry> getWaitlistByStudent(int studentId) {
        List<WaitlistEntry> waitlist = new ArrayList<>();

        String query =
                "SELECT ws.waitlist_id, wd.priority_score, wd.joined_at, wd.slot_id, " +
                "ue.user_id, ue.email, ud.password_hash, ud.first_name, ud.last_name, " +
                "ud.phone_number, s.year, " +
                "t.slot_date, t.professor_id, t.start_time, t.end_time, t.status, " +
                "t.reserved_count, t.current_bookings, t.max_capacity, t.is_manually_blocked_by_prof " +
                "FROM waitlisted_student ws " +
                "JOIN waitlist_details wd ON ws.waitlist_id = wd.waitlist_id " +
                "JOIN user_email ue ON ws.student_id = ue.user_id " +
                "JOIN user_details ud ON ue.user_id = ud.user_id " +
                "JOIN student s ON ue.user_id = s.student_id " +
                "JOIN timeslot t ON wd.slot_id = t.slot_id " +
                "WHERE ws.student_id = ? " +
                "ORDER BY wd.priority_score DESC, wd.joined_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Student student = mapStudentFromResultSet(rs);
                TimeSlot slot = mapTimeSlotFromResultSet(rs);

                WaitlistEntry entry = new WaitlistEntry(
                        rs.getInt("waitlist_id"),
                        student,
                        slot,
                        rs.getInt("priority_score"),
                        rs.getTimestamp("joined_at").toLocalDateTime()
                );
                waitlist.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return waitlist;
    }

    // Remove student from waitlist
    public boolean removeFromWaitlist(
            int waitlistId) {

        String deleteDetailsQuery =
                "DELETE FROM waitlist_details " +
                "WHERE waitlist_id = ?";

        String deleteStudentQuery =
                "DELETE FROM waitlisted_student " +
                "WHERE waitlist_id = ?";

        try (Connection conn =
                     DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            // Delete details first
            PreparedStatement ps1 =
                    conn.prepareStatement(
                            deleteDetailsQuery
                    );

            ps1.setInt(1, waitlistId);

            ps1.executeUpdate();

            // Delete parent record
            PreparedStatement ps2 =
                    conn.prepareStatement(
                            deleteStudentQuery
                    );

            ps2.setInt(1, waitlistId);

            int rowsDeleted =
                    ps2.executeUpdate();

            if (rowsDeleted > 0) {

                conn.commit();
                return true;

            } else {

                conn.rollback();
                return false;
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return false;
    }

    // Get highest priority student
    public WaitlistEntry
    getHighestPriorityStudent(int slotId) {

        String query =
                "SELECT ws.waitlist_id, wd.priority_score, wd.joined_at, wd.slot_id, " +
                "ue.user_id, ue.email, ud.password_hash, ud.first_name, ud.last_name, " +
                "ud.phone_number, s.year, " +
                "t.slot_date, t.professor_id, t.start_time, t.end_time, t.status, " +
                "t.reserved_count, t.current_bookings, t.max_capacity, t.is_manually_blocked_by_prof " +
                "FROM waitlisted_student ws " +
                "JOIN waitlist_details wd ON ws.waitlist_id = wd.waitlist_id " +
                "JOIN user_email ue ON ws.student_id = ue.user_id " +
                "JOIN user_details ud ON ue.user_id = ud.user_id " +
                "JOIN student s ON ue.user_id = s.student_id " +
                "JOIN timeslot t ON wd.slot_id = t.slot_id " +
                "WHERE wd.slot_id = ? " +
                "ORDER BY wd.priority_score DESC, wd.joined_at ASC " +
                "LIMIT 1";

        try (Connection conn =
                     DBConnection.getConnection();

             PreparedStatement ps =
                     conn.prepareStatement(query)) {

            ps.setInt(1, slotId);

            ResultSet rs =
                    ps.executeQuery();

            if (rs.next()) {

                Student student = mapStudentFromResultSet(rs);
                TimeSlot slot = mapTimeSlotFromResultSet(rs);

                return new WaitlistEntry(
                        rs.getInt("waitlist_id"),
                        student,
                        slot,
                        rs.getInt("priority_score"),
                        rs.getTimestamp("joined_at").toLocalDateTime()
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return null;
    }

    private Student mapStudentFromResultSet(ResultSet rs)
            throws SQLException {
        int studentId = rs.getInt("user_id");
        String email = rs.getString("email");
        String password = rs.getString("password_hash");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String phone = rs.getString("phone_number");
        int year = rs.getInt("year");
        return new Student(studentId, email, password,
                firstName, lastName, phone, year);
    }

    private TimeSlot mapTimeSlotFromResultSet(ResultSet rs)
            throws SQLException {
        TimeSlot slot = new TimeSlot(
                rs.getInt("slot_id"),
                rs.getDate("slot_date").toLocalDate(),
                rs.getInt("professor_id"),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime()
        );
        slot.setStatus(TimeSlotStatus.valueOf(rs.getString("status")));
        slot.setReservedCount(rs.getInt("reserved_count"));
        slot.setCurrentBookings(rs.getInt("current_bookings"));
        slot.setMaxCapacity(rs.getInt("max_capacity"));
        slot.setIsManuallyBlockedByProf(rs.getBoolean("is_manually_blocked_by_prof"));
        return slot;
    }

    // Remove by student and slot
    public boolean removeByStudentAndSlot(int studentId, int slotId) {
        String query = "SELECT waitlist_id FROM waitlist_details WHERE slot_id = ? AND waitlist_id IN (SELECT waitlist_id FROM waitlisted_student WHERE student_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, slotId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int waitlistId = rs.getInt("waitlist_id");
                return removeFromWaitlist(waitlistId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get waitlist entry by id
    public WaitlistEntry getWaitlistEntryById(int waitlistId) {
        String query = "SELECT ws.waitlist_id, wd.priority_score, wd.joined_at, wd.slot_id, " +
                       "ue.user_id, ue.email, ud.password_hash, ud.first_name, ud.last_name, " +
                       "ud.phone_number, s.year, " +
                       "t.slot_date, t.professor_id, t.start_time, t.end_time, t.status, " +
                       "t.reserved_count, t.current_bookings, t.max_capacity, t.is_manually_blocked_by_prof " +
                       "FROM waitlisted_student ws " +
                       "JOIN waitlist_details wd ON ws.waitlist_id = wd.waitlist_id " +
                       "JOIN user_email ue ON ws.student_id = ue.user_id " +
                       "JOIN user_details ud ON ue.user_id = ud.user_id " +
                       "JOIN student s ON ue.user_id = s.student_id " +
                       "JOIN timeslot t ON wd.slot_id = t.slot_id " +
                       "WHERE ws.waitlist_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, waitlistId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Student student = mapStudentFromResultSet(rs);
                TimeSlot slot = mapTimeSlotFromResultSet(rs);
                return new WaitlistEntry(
                        rs.getInt("waitlist_id"),
                        student,
                        slot,
                        rs.getInt("priority_score"),
                        rs.getTimestamp("joined_at").toLocalDateTime()
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
