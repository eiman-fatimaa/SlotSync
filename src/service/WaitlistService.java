package service;

import dao.AppointmentDAO;
import dao.TimeSlotDAO;
import dao.UserDAO;
import dao.WaitlistDAO;
import enums.AppointmentStatus;
import model.Appointment;
import model.Student;
import model.TimeSlot;
import model.WaitlistEntry;
import priority.PriorityCalculator;

import java.time.LocalDateTime;
import java.util.List;

public class WaitlistService {

    private WaitlistDAO waitlistDAO;
    private AppointmentDAO appointmentDAO;
    private UserDAO userDAO;
    private TimeSlotDAO timeSlotDAO;

    public WaitlistService() {
        this.waitlistDAO = new WaitlistDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.userDAO = new UserDAO();
        this.timeSlotDAO = new TimeSlotDAO();
    }

    // Add student to waitlist (called by professor to waitlist a pending appointment)
    public boolean addToWaitlist(int appointmentId) {
        // Get the appointment
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment == null || appointment.getStatus() != AppointmentStatus.PENDING) {
            return false; // Only pending appointments can be waitlisted
        }

        // Get student and slot
        Student student = (Student) userDAO.getUserById(appointment.getStudentId());
        TimeSlot slot = timeSlotDAO.getSlotById(appointment.getSlotId());

        if (student == null || slot == null) {
            return false;
        }

        int priorityScore = PriorityCalculator.calculatePriorityScore(appointment, student);

        // Update appointment status to WAITLISTED
        boolean statusUpdated = appointmentDAO.updateAppointmentStatus(appointmentId, AppointmentStatus.WAITLISTED);
        if (!statusUpdated) {
            return false;
        }

        // Create WaitlistEntry
        WaitlistEntry entry = new WaitlistEntry(
                0, // waitlistId will be generated
                student,
                slot,
                priorityScore,
                LocalDateTime.now()
        );

        // Add to DAO
        return waitlistDAO.addToWaitlist(entry);
    }

    // Remove student from waitlist (by professor)
    public boolean removeFromWaitlist(int waitlistId) {
        // Get the waitlist entry to find student and slot
        WaitlistEntry entry = waitlistDAO.getWaitlistEntryById(waitlistId);
        if (entry == null) {
            return false;
        }

        // Find the appointment and set status to CANCELLED
        Appointment appointment = appointmentDAO.getAppointmentByStudentAndSlot(entry.getStudent().getUserId(), entry.getSlot().getSlotID());
        if (appointment != null) {
            appointmentDAO.updateAppointmentStatus(appointment.getAppointmentId(), AppointmentStatus.CANCELLED);
        }

        // Remove from waitlist
        return waitlistDAO.removeFromWaitlist(waitlistId);
    }

    // Handle appointment cancellation: if waitlisted, remove from waitlist
    public void handleAppointmentCancellation(int appointmentId) {
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment != null && appointment.getStatus() == AppointmentStatus.WAITLISTED) {
            // Remove from waitlist
            waitlistDAO.removeByStudentAndSlot(appointment.getStudentId(), appointment.getSlotId());
        }
    }

    // Promote highest priority student from waitlist to approved when an approved appointment is cancelled
    public void promoteFromWaitlist(int slotId) {
        WaitlistEntry highest = waitlistDAO.getHighestPriorityStudent(slotId);
        if (highest != null) {
            // Find the appointment for this student and slot
            Appointment appointment = appointmentDAO.getAppointmentByStudentAndSlot(highest.getStudent().getUserId(), slotId);
            if (appointment != null && appointment.getStatus() == AppointmentStatus.WAITLISTED) {
                // Update to APPROVED
                appointmentDAO.updateAppointmentStatus(appointment.getAppointmentId(), AppointmentStatus.APPROVED);
                // Remove from waitlist
                waitlistDAO.removeFromWaitlist(highest.getWaitlistId());
            }
        }
    }

    // Get waitlist for a slot
    public List<WaitlistEntry> getWaitlistBySlot(int slotId) {
        return waitlistDAO.getWaitlistBySlot(slotId);
    }

    // Get waitlist for a student
    public List<WaitlistEntry> getWaitlistByStudent(int studentId) {
        return waitlistDAO.getWaitlistByStudent(studentId);
    }
}