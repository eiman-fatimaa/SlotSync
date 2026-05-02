package model;


import java.time.LocalDate;
import java.time.LocalTime;
import enums.TimeSlotStatus;

public class TimeSlot {

    // all the instance variables are declared private
    private int slot_id;
    private LocalDate slot_date;
    private int professor_id;
    private LocalTime start_time;
    private LocalTime end_time;
    private TimeSlotStatus status; 
    private int reserved_count;
    private int current_bookings;
    private int max_capacity;
    private boolean is_manually_blocked_by_prof;

    // constructor
    public TimeSlot(int sid, LocalDate sd, int pid, LocalTime st, LocalTime et) {
        slot_id = sid;
        slot_date = sd;
        professor_id = pid;
        start_time = st;
        end_time = et;
        status = TimeSlotStatus.FREE;
        reserved_count = 0;
        current_bookings = 0;
        max_capacity = 3;
        is_manually_blocked_by_prof = false;
    }
    

    // getter, setter methods for all instance variables

    // for slot_id

    public void setSlotID(int s_id) {
        slot_id = s_id;
    }

    public int getSlotID() {
        return slot_id;
    }

    // for slot_date

    public void setSlotDate(LocalDate d) {

        slot_date = d;
    }

    public LocalDate getSlotDate() {
        
        return slot_date;
    }

    // for professor_id

    public void setProfessorID(int pid) {

        professor_id = pid;
    }

    public int getProfessorID() {
        
        return professor_id;
    }

    // for start_time

    public void setStartTime(LocalTime st) {

        start_time = st;
    }

    public LocalTime getStartTime() {
        
        return start_time;
    }

    // for end_time

    public void setEndTime(LocalTime et) {

        end_time = et;
    }

    public LocalTime getEndTime() {
        
        return end_time;
    }

    // for status

    public void setStatus(TimeSlotStatus s) {

        status = s;
    }

    public TimeSlotStatus getStatus() {
        
        return status;
    }

    // for reserved_count
    
    public void setReservedCount(int rc) {

        reserved_count = rc;
    }

    public int getReservedCount() {
        
        return reserved_count;
    }

    // for current_bookings
    
    public void setCurrentBookings(int cb) {

        current_bookings = cb;
    }

    public int getCurrentBookings() {
        
        return current_bookings;
    }

    // for max_capacity
    
    public void setMaxCapacity(int mc) {

        max_capacity = mc;
    }

    public int getMaxCapacity() {
        
        return max_capacity;
    }

    // for is_manually_blocked_by_prof
    
    public void setIsManuallyBlockedByProf(boolean blocked) {

        is_manually_blocked_by_prof = blocked;
    }

    public boolean getIsManuallyBlockedByProf() {
        
        return is_manually_blocked_by_prof;
    }

}
