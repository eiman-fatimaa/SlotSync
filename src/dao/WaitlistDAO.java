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

// DAO for managing waitlist operations
public class WaitlistDAO {

    // Add student to waitlist
    public boolean addToWaitlist(WaitlistEntry entry) {

        // Two-step process:
        //queries
        String insertStudentQuery =
                "INSERT INTO waitlisted_student(student_id) VALUES (?)";

        String insertDetailsQuery =
                "INSERT INTO waitlist_details " +
                "(waitlist_id, priority_score, joined_at, slot_id) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {  //try-with-resources for auto-closing and preventing memory leaks

                // Start transaction
            conn.setAutoCommit(false);

            //STEP 1: add a Student to Waitlisted_Student to get waitlist_id
            //Prepare statement for protection against SQL injection and to retrieve generated keys
            PreparedStatement ps1 =
                    conn.prepareStatement(
                            insertStudentQuery,
                            Statement.RETURN_GENERATED_KEYS       //to get the auto-generated waitlist_id
                    );

            //set student_id parameter
            ps1.setInt(
                    1,
                    entry.getStudent().getUserId()    //get student_id from the Student object in the WaitlistEntry
            );

            int affectedRows = ps1.executeUpdate();     //execute the insert and get the number of affected rows

            if (affectedRows == 0) {            //if no rows were inserted, something went wrong
                conn.rollback();                //rollback transaction to maintain data integrity
                return false;                   //indicate failure to add to waitlist
            }

            //get generated waitlist_id
            ResultSet generatedKeys =           //retrieve the auto-generated keys from the insert operation
                    ps1.getGeneratedKeys();

            int waitlistId;             //variable to hold the generated waitlist_id

            if (generatedKeys.next()) {         //if there is a generated key, retrieve it

                waitlistId =                    //get the first generated key (waitlist_id) from the result set
                        generatedKeys.getInt(1);

            } else {

                conn.rollback();                //if no key was generated, rollback transaction and indicate failure
                return false;
            }

            //STEP 2: insert details into waitlist_details using the generated waitlist_id
            PreparedStatement ps2 =
                    conn.prepareStatement(              //prepare statement for inserting waitlist details
                            insertDetailsQuery
                    );

            ps2.setInt(1, waitlistId);                  //set the waitlist_id parameter to the generated waitlistId from step 1

            ps2.setInt(
                    2,
                    entry.getPriorityScore()            //set the priority_score parameter from the WaitlistEntry object
            );

            ps2.setTimestamp(                           //set the joined_at parameter using the joinedAt field from the WaitlistEntry, 
                                                        //converting it to a Timestamp for SQL
                    3,
                    Timestamp.valueOf(
                            entry.getJoinedAt()
                    )
            );

            ps2.setInt(
                    4,
                    entry.getSlot().getSlotID()         //set the slot_id parameter from the TimeSlot object in the WaitlistEntry
            );

            int rowsInserted =
                    ps2.executeUpdate();                //execute the insert for waitlist details and get the number of affected rows

            if (rowsInserted > 0) {                     //if details were successfully inserted, commit and indicate success

                conn.commit();
                return true;

            } else {                                    //if no rows inserted, something went wrong

                conn.rollback();                        //rollback transaction
                return false;                           //indicate failure to add to waitlist   
            }

        } catch (SQLException e) {                      //catch any SQL exceptions 

            e.printStackTrace();                        //print stack trace for debugging
            return false;
        }
    }

    //check if student already waitlisted for a specific slot, preventing duplicates
    //used in the service layer
    public boolean isStudentWaitlisted(int studentId,int slotId) {

        //query
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
                     conn.prepareStatement(query)) {            //prep statement

            ps.setInt(1, studentId);                            //setting parameters
            ps.setInt(2, slotId);

            ResultSet rs =
                    ps.executeQuery();                           //execute query and get result set

            return rs.next();                                    //if there is a result, the student is already waitlisted for that slot, return true   

        } catch (SQLException e) {                               //catch exceptions

            e.printStackTrace();                                //print stack trace for debugging
        }

        return false;                                           //otherwise, student is not waitlisted
    }

    //Get all waitlisted students for a slot
    public List<WaitlistEntry> getWaitlistBySlot(int slotId) {

        List<WaitlistEntry> waitlist = new ArrayList<>();               //array list to hold waitlist entries

        //query
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

            ps.setInt(1, slotId);               //set slotId

            ResultSet rs =
                    ps.executeQuery();          //execute query and get result set

            while (rs.next()) {                 //iterate through result set

                Student student = mapStudentFromResultSet(rs);          //map student data from result set to Student object
                TimeSlot slot = mapTimeSlotFromResultSet(rs);           //map timeslot data from result set to TimeSlot object

                WaitlistEntry entry =                      //create new WaitlistEntry object using the retrieved data, including the mapped Student and TimeSlot objects
                        new WaitlistEntry(
                                rs.getInt("waitlist_id"),
                                student,
                                slot,
                                rs.getInt("priority_score"),
                                rs.getTimestamp("joined_at").toLocalDateTime()         //convert joined_at timestamp to LocalDateTime for the WaitlistEntry constructor
                        );

                waitlist.add(entry);                            //add the created WaitlistEntry to the waitlist array
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return waitlist;                                //return the list of waitlist entries for the specified slot
    }

    //get waitlist entries for a student
    public List<WaitlistEntry> getWaitlistByStudent(int studentId) {          //used in service layer to show students their waitlisted slots
        List<WaitlistEntry> waitlist = new ArrayList<>();

        //query
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

            ps.setInt(1, studentId);            //set studentId parameter
            ResultSet rs = ps.executeQuery();     //execute query and get result set

            while (rs.next()) {                 //iterate through result set and map data to WaitlistEntry objects
                Student student = mapStudentFromResultSet(rs);          //map student data from result set to Student object
                TimeSlot slot = mapTimeSlotFromResultSet(rs);           //map timeslot data from result set to TimeSlot object

                WaitlistEntry entry = new WaitlistEntry(                //create new WaitlistEntry object using the retrieved data, including the mapped Student and TimeSlot objects
                        rs.getInt("waitlist_id"),
                        student,
                        slot,
                        rs.getInt("priority_score"),
                        rs.getTimestamp("joined_at").toLocalDateTime()
                );
                waitlist.add(entry);                    //add` the created WaitlistEntry to the waitlist array
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return waitlist;                //return the list
    }

    //Remove student from waitlist              //used in service layer when a student cancels their waitlist entry or when they are moved from waitlist to booked for a slot
    public boolean removeFromWaitlist(
            int waitlistId) {

        //queries to delete from both waitlisted_student and waitlist_details 
        String deleteDetailsQuery =
                "DELETE FROM waitlist_details " +
                "WHERE waitlist_id = ?";

        String deleteStudentQuery =
                "DELETE FROM waitlisted_student " +
                "WHERE waitlist_id = ?";

        try (Connection conn =
                     DBConnection.getConnection()) {

            conn.setAutoCommit(false);          //start transaction to ensure both deletes succeed or fail together

            //Delete details first since it has a foreign key reference to waitlisted_student
            PreparedStatement ps1 =
                    conn.prepareStatement(
                            deleteDetailsQuery
                    );

            ps1.setInt(1, waitlistId);          //set waitlistId parameter for details deletion

            ps1.executeUpdate();                //execute delete for waitlist details

            //Delete parent record
            PreparedStatement ps2 =
                    conn.prepareStatement(
                            deleteStudentQuery
                    );

            ps2.setInt(1, waitlistId);                  //set waitlistId

            int rowsDeleted =
                    ps2.executeUpdate();                //execute delete for waitlisted_student and get number of affected rows

            if (rowsDeleted > 0) {

                conn.commit();                  //commit transaction if both deletes succeed
                return true;

            } else {

                conn.rollback();                //rollback transaction if no rows were deleted 
                return false;
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return false;           //indicate failure to remove from waitlist
    }

    //Get highest priority student       //used in service layer when a slot becomes available to find the next student to move from waitlist to booked for that slot
    public WaitlistEntry getHighestPriorityStudent(int slotId) {
        //query
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
                    ps.executeQuery();          //execute query and get result set

            if (rs.next()) {                    //if there is a result, map data to a WaitlistEntry object and return it

                Student student = mapStudentFromResultSet(rs);          //map student data from result set to Student object
                TimeSlot slot = mapTimeSlotFromResultSet(rs);           //map timeslot data from result set to TimeSlot object

                return new WaitlistEntry(                               //create and return a new WaitlistEntry object using the retrieved data, including the mapped Student and TimeSlot objects
                        rs.getInt("waitlist_id"),
                        student,
                        slot,
                        rs.getInt("priority_score"),
                        rs.getTimestamp("joined_at").toLocalDateTime()          //convert joined_at timestamp to LocalDateTime for the WaitlistEntry constructor        
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return null;                            //if no waitlisted students for the slot, return null
    }

    private Student mapStudentFromResultSet(ResultSet rs)    //helper method to map student data from result set to Student object
            throws SQLException {
        int studentId = rs.getInt("user_id");           //get all columns from result set
        String email = rs.getString("email");
        String password = rs.getString("password_hash");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String phone = rs.getString("phone_number");
        int year = rs.getInt("year");
        return new Student(studentId, email, password,          //create and return new Student object using the retrieved data
                firstName, lastName, phone, year);
    }

    private TimeSlot mapTimeSlotFromResultSet(ResultSet rs)     //helper method to map student data from result set to Student object
            throws SQLException {
        TimeSlot slot = new TimeSlot(                           //create new TimeSlot object using the retrieved data
                rs.getInt("slot_id"),
                rs.getDate("slot_date").toLocalDate(),
                rs.getInt("professor_id"),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime()
        );
        slot.setStatus(TimeSlotStatus.valueOf(rs.getString("status")));         //set details for the TimeSlot object
        slot.setReservedCount(rs.getInt("reserved_count"));
        slot.setCurrentBookings(rs.getInt("current_bookings"));
        slot.setMaxCapacity(rs.getInt("max_capacity"));
        slot.setIsManuallyBlockedByProf(rs.getBoolean("is_manually_blocked_by_prof"));
        return slot;                    //return the mapped TimeSlot object
    }

    //remove by student and slot
    public boolean removeByStudentAndSlot(int studentId, int slotId) {          //used in service layer when a student cancels their waitlist entry for a specific slot, allowing them to specify the slot rather than needing the waitlistId
        String query = "SELECT waitlist_id FROM waitlist_details WHERE slot_id = ? AND waitlist_id IN (SELECT waitlist_id FROM waitlisted_student WHERE student_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, slotId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {            //if there is a result, get the waitlist_id
                int waitlistId = rs.getInt("waitlist_id");
                return removeFromWaitlist(waitlistId);          //call the existing removeFromWaitlist method to perform the deletion using the retrieved waitlistId
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //get waitlist entry by id                  //used in service layer when a student is moved from waitlist to booked for a slot, allowing retrieval of the waitlist entry details using the waitlistId to then create the booking
    public WaitlistEntry getWaitlistEntryById(int waitlistId) {
        //query
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
            ps.setInt(1, waitlistId);           //set parameter for waitlistId
            ResultSet rs = ps.executeQuery();   //execute query to get result set
            if (rs.next()) {            //if there is a result, map data to a WaitlistEntry object and return it
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
        return null;                    //if no entry found for the given waitlistId, return null
    }
}
