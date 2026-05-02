package dao;

import model.TimeSlot;
import enums.TimeSlotStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TimeSlotDAO {

    // gets slots by a date range
    public List<TimeSlot> getSlotsByDateRange(int professorId, LocalDate startDate, LocalDate endDate) {

        String query = """
            SELECT slot_id, slot_date, professor_id, start_time, end_time, status, reserved_count, current_bookings, max_capacity, is_manually_blocked_by_prof
            FROM timeslot
            WHERE professor_id = ? AND slot_date BETWEEN ? AND ?
            ORDER BY slot_date, start_time
        """;

        List<TimeSlot> slots = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, professorId);
            stmt.setDate(2, java.sql.Date.valueOf(startDate));
            stmt.setDate(3, java.sql.Date.valueOf(endDate));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int slotId = rs.getInt("slot_id");
                LocalDate slotDate = rs.getDate("slot_date").toLocalDate();
                int profId = rs.getInt("professor_id");
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalTime endTime = rs.getTime("end_time").toLocalTime();
                TimeSlotStatus status = TimeSlotStatus.valueOf(rs.getString("status"));
                int reservedCount = rs.getInt("reserved_count");
                int currentBookings = rs.getInt("current_bookings");
                int maxCapacity = rs.getInt("max_capacity");
                boolean isBlocked = rs.getBoolean("is_manually_blocked_by_prof");

                TimeSlot slot = new TimeSlot(slotId, slotDate, profId, startTime, endTime);
                slot.setStatus(status);
                slot.setReservedCount(reservedCount);
                slot.setCurrentBookings(currentBookings);
                slot.setMaxCapacity(maxCapacity);
                slot.setIsManuallyBlockedByProf(isBlocked);
                slots.add(slot);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return slots;
    }

    // gets all the past slots of a professor
    public List<TimeSlot> getPastSlots(int professorId) {
        LocalDate today = LocalDate.now();
        return getSlotsByDateRange(professorId, LocalDate.MIN, today.minusDays(1));
    }

    // gets the slots for this week,uses getSlotsByDateRange
    public List<TimeSlot> getThisWeekSlots(int professorId) {
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
        return getSlotsByDateRange(professorId, start, start.plusDays(6));
    }

    // gets the slots for next week, uses getSlotsByDateRange
    public List<TimeSlot> getNextWeekSlots(int professorId) {
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);
        return getSlotsByDateRange(professorId, start, start.plusDays(6));
    }

    // gets all the slots of a specific date
    public List<TimeSlot> getSlotsByDate(LocalDate date) {
        String query = """
            SELECT slot_id, slot_date, professor_id, start_time, end_time, status, reserved_count, current_bookings, max_capacity, is_manually_blocked_by_prof
            FROM timeslot
            WHERE slot_date = ?
            ORDER BY start_time
        """;

        List<TimeSlot> slots = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, java.sql.Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int slotId = rs.getInt("slot_id");
                LocalDate slotDate = rs.getDate("slot_date").toLocalDate();
                int profId = rs.getInt("professor_id");
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalTime endTime = rs.getTime("end_time").toLocalTime();
                TimeSlotStatus status = TimeSlotStatus.valueOf(rs.getString("status"));
                int reservedCount = rs.getInt("reserved_count");
                int currentBookings = rs.getInt("current_bookings");
                int maxCapacity = rs.getInt("max_capacity");
                boolean isBlocked = rs.getBoolean("is_manually_blocked_by_prof");

                TimeSlot slot = new TimeSlot(slotId, slotDate, profId, startTime, endTime);
                slot.setStatus(status);
                slot.setReservedCount(reservedCount);
                slot.setCurrentBookings(currentBookings);
                slot.setMaxCapacity(maxCapacity);
                slot.setIsManuallyBlockedByProf(isBlocked);
                slots.add(slot);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return slots;
    }

    // gets all the upcoming slots of a professor
    public List<TimeSlot> getUpcomingSlotsByProfessor(int professorId) {
        LocalDate today = LocalDate.now();
        LocalDate farFuture = today.plusYears(5); 
        return getSlotsByDateRange(professorId, today, farFuture);
    }

    // gets all the upcoming slots, irrespective of professor
    public List<TimeSlot> getAllUpcomingSlots() {
        String query = """
            SELECT slot_id, slot_date, professor_id, start_time, end_time, status, reserved_count, current_bookings, max_capacity, is_manually_blocked_by_prof
            FROM timeslot
            WHERE slot_date >= ?
            ORDER BY slot_date, start_time
        """;

        List<TimeSlot> slots = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int slotId = rs.getInt("slot_id");
                LocalDate slotDate = rs.getDate("slot_date").toLocalDate();
                int profId = rs.getInt("professor_id");
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalTime endTime = rs.getTime("end_time").toLocalTime();
                TimeSlotStatus status = TimeSlotStatus.valueOf(rs.getString("status"));
                int reservedCount = rs.getInt("reserved_count");
                int currentBookings = rs.getInt("current_bookings");
                int maxCapacity = rs.getInt("max_capacity");
                boolean isBlocked = rs.getBoolean("is_manually_blocked_by_prof");

                TimeSlot slot = new TimeSlot(slotId, slotDate, profId, startTime, endTime);
                slot.setStatus(status);
                slot.setReservedCount(reservedCount);
                slot.setCurrentBookings(currentBookings);
                slot.setMaxCapacity(maxCapacity);
                slot.setIsManuallyBlockedByProf(isBlocked);
                slots.add(slot);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return slots;
    }


    // allows the professor to block a slot
    public boolean blockSlot(int slotId) {
        String query = "UPDATE timeslot SET status = 'LOCKED', is_manually_blocked_by_prof = true WHERE slot_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, slotId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // allows the professor to unblock a slot
    public boolean unblockSlot(int slotId) {
        String query = "UPDATE timeslot SET status = 'FREE', is_manually_blocked_by_prof = false WHERE slot_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, slotId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // allows the slot status to be updated
    public boolean updateSlotStatus(int slotId, TimeSlotStatus newStatus) {
        String query = "UPDATE timeslot SET status = ? WHERE slot_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, newStatus.name());
            stmt.setInt(2, slotId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // fetches slots by their id
    public TimeSlot getSlotById(int slotId) {
        String query = """
            SELECT slot_id, slot_date, professor_id, start_time, end_time, status, reserved_count, current_bookings, max_capacity, is_manually_blocked_by_prof
            FROM timeslot
            WHERE slot_id = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, slotId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                LocalDate slotDate = rs.getDate("slot_date").toLocalDate();
                int profId = rs.getInt("professor_id");
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalTime endTime = rs.getTime("end_time").toLocalTime();
                TimeSlotStatus status = TimeSlotStatus.valueOf(rs.getString("status"));
                int reservedCount = rs.getInt("reserved_count");
                int currentBookings = rs.getInt("current_bookings");
                int maxCapacity = rs.getInt("max_capacity");
                boolean isBlocked = rs.getBoolean("is_manually_blocked_by_prof");

                TimeSlot slot = new TimeSlot(slotId, slotDate, profId, startTime, endTime);
                slot.setStatus(status);
                slot.setReservedCount(reservedCount);
                slot.setCurrentBookings(currentBookings);
                slot.setMaxCapacity(maxCapacity);
                slot.setIsManuallyBlockedByProf(isBlocked);
                return slot;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    

    // some of the methods are not used, like the week ones. 

    // i made these additional methods, later found out that they have already been implemented in the appointment DAO
    // public List<TimeSlot> getAvailableSlots() {
    //     String query = """
    //         SELECT slot_id, slot_date, professor_id, start_time, end_time, status, reserved_count, current_bookings, max_capacity, is_manually_blocked_by_prof
    //         FROM timeslot
    //         WHERE (status = 'FREE' OR status = 'PARTIALLY_BOOKED') AND is_manually_blocked_by_prof = false AND slot_date >= ?
    //         ORDER BY slot_date, start_time
    //     """;

    //     List<TimeSlot> slots = new ArrayList<>();
    //     try (Connection conn = DBConnection.getConnection();
    //          PreparedStatement stmt = conn.prepareStatement(query)) {

    //         stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));

    //         ResultSet rs = stmt.executeQuery();
    //         while (rs.next()) {
    //             int slotId = rs.getInt("slot_id");
    //             LocalDate slotDate = rs.getDate("slot_date").toLocalDate();
    //             int profId = rs.getInt("professor_id");
    //             LocalTime startTime = rs.getTime("start_time").toLocalTime();
    //             LocalTime endTime = rs.getTime("end_time").toLocalTime();
    //             TimeSlotStatus status = TimeSlotStatus.valueOf(rs.getString("status"));
    //             int reservedCount = rs.getInt("reserved_count");
    //             int currentBookings = rs.getInt("current_bookings");
    //             int maxCapacity = rs.getInt("max_capacity");
    //             boolean isBlocked = rs.getBoolean("is_manually_blocked_by_prof");

    //             TimeSlot slot = new TimeSlot(slotId, slotDate, profId, startTime, endTime);
    //             slot.setStatus(status);
    //             slot.setReservedCount(reservedCount);
    //             slot.setCurrentBookings(currentBookings);
    //             slot.setMaxCapacity(maxCapacity);
    //             slot.setIsManuallyBlockedByProf(isBlocked);
    //             slots.add(slot);
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    //     return slots;
    // }

    

    // public boolean bookSlot(int slotId) {
    //     String query = """
    //         UPDATE timeslot
    //         SET current_bookings = current_bookings + 1,
    //             status = CASE
    //                 WHEN current_bookings + 1 >= max_capacity THEN 'LOCKED'
    //                 ELSE 'PARTIALLY_BOOKED'
    //             END
    //         WHERE slot_id = ?
    //           AND current_bookings < max_capacity
    //           AND is_manually_blocked_by_prof = false
    //     """;

    //     try (Connection conn = DBConnection.getConnection();
    //          PreparedStatement stmt = conn.prepareStatement(query)) {

    //         stmt.setInt(1, slotId);
    //         int rowsAffected = stmt.executeUpdate();
    //         return rowsAffected > 0;
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return false;
    //     }
    // }
}



