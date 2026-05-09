package model;

import java.time.LocalDateTime;

public class WaitlistEntry {

    private int waitlistId;
    private Student student;
    private TimeSlot slot;
    private int priorityScore;
    private LocalDateTime joinedAt;

    public WaitlistEntry(int waitlistId,
                         Student student,
                         TimeSlot slot,
                         int priorityScore,
                         LocalDateTime joinedAt) {

        this.waitlistId = waitlistId;
        this.student = student;
        this.slot = slot;
        this.priorityScore = priorityScore;
        this.joinedAt = joinedAt;
    }

    // Getters and Setters

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