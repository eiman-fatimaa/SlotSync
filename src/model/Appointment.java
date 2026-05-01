package model;

import enums.AppointmentReason;
import enums.AppointmentStatus;
import java.time.LocalDateTime;

public class Appointment {
    private int appointmentId;
    private int studentId;
    private int slotId;
    private AppointmentStatus status;
    private AppointmentReason reason;
    private String note;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private Integer rescheduledFrom;

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
}