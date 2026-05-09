package model;

import enums.AppointmentReason;
import enums.AppointmentStatus;
import java.time.LocalDateTime;
//represnets a single appt btw a student and prof
public class Appointment {
    //core appt data members
    private int appointmentId;
    private int studentId;
    private int slotId;
    private AppointmentStatus status;//pending, approved etc
    private AppointmentReason reason;//reason for appt
    private String note;//optional note from student
    private String rejectionReason;//filled by prof if rejected
    private LocalDateTime createdAt;//when appt boked
    private Integer rescheduledFrom;//id of original appt if rescheduled
    //extra field populated from timeslot and user_details join for display purposes
    private String professorName;
    private java.time.LocalDate slotDate;
    private java.time.LocalTime slotStartTime;
    private java.time.LocalTime slotEndTime;

    //constructor
    public Appointment(int appointmentId, int studentId, int slotId,
                       AppointmentStatus status, AppointmentReason reason,
                       String note, LocalDateTime createdAt) {
        this.appointmentId = appointmentId;
        this.studentId = studentId;
        this.slotId = slotId;
        this.status = status;
        this.reason = reason;
        this.note = note;
        this.createdAt = createdAt;
    }
    //getter and setters
    public int getAppointmentId()       { return appointmentId; }
    public int getStudentId()           { return studentId; }
    public int getSlotId()              { return slotId; }
    public AppointmentStatus getStatus(){ return status; }
    public void setStatus(AppointmentStatus s) { this.status = s; }
    public AppointmentReason getReason(){ return reason; }
    public String getNote()             { return note; }
    public String getRejectionReason()  { return rejectionReason; }
    public void setRejectionReason(String r) { this.rejectionReason = r; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getRescheduledFrom() { return rescheduledFrom; }
    public void setRescheduledFrom(Integer r) { this.rescheduledFrom = r; }
    public String getProfessorName()            { return professorName; }
    public void setProfessorName(String n)      { this.professorName = n; }
    public java.time.LocalDate getSlotDate()    { return slotDate; }
    public void setSlotDate(java.time.LocalDate d) { this.slotDate = d; }
    public java.time.LocalTime getSlotStartTime()  { return slotStartTime; }
    public void setSlotStartTime(java.time.LocalTime t) { this.slotStartTime = t; }
    public java.time.LocalTime getSlotEndTime()    { return slotEndTime; }
    public void setSlotEndTime(java.time.LocalTime t)   { this.slotEndTime = t; }
}