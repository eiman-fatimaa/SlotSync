package model;

import java.time.LocalDateTime;

public class WaitlistEntry {

    private int waitlistId;         //waitlist_id from waitlist_details table
    private Student student;        //associated student for the waitlist entry
    private TimeSlot slot;          //associated time slot for the waitlist entry
    private int priorityScore;      //priority score for the waitlist entry, calculated based on the defined criteria
    private LocalDateTime joinedAt; //timestamp of when the student joined the waitlist, used for tie-breaking when priority scores are equal

    public WaitlistEntry(int waitlistId, Student student, TimeSlot slot, int priorityScore, LocalDateTime joinedAt) {
        //Initialize all fields in the constructor
        this.waitlistId = waitlistId;
        this.student = student;
        this.slot = slot;
        this.priorityScore = priorityScore;
        this.joinedAt = joinedAt;
    }

    // Getters
    //No setters provided as the waitlist entry details should not be modified after creation 
    public int getWaitlistId() {
        return waitlistId;
    }

    public Student getStudent() {
        return student;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}