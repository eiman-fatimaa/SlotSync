package service;

import java.time.LocalDateTime;
import java.util.List;

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
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment == null || appointment.getStatus() != AppointmentStatus.PENDING) {
            return false;
        }

        Student student = (Student) userDAO.getUserById(appointment.getStudentId());
        TimeSlot slot = timeSlotDAO.getSlotById(appointment.getSlotId());

        if (student == null || slot == null) {
            return false;
        }

        int priorityScore = PriorityCalculator.calculatePriorityScore(appointment, student);

        boolean statusUpdated = appointmentDAO.updateAppointmentStatus(
            appointmentId, AppointmentStatus.WAITLISTED);
        if (!statusUpdated) {
            return false;
        }

        WaitlistEntry entry = new WaitlistEntry(
            0,
            student,
            slot,
            priorityScore,
            LocalDateTime.now()
        );

        return waitlistDAO.addToWaitlist(entry);
    }

    // Remove student from waitlist (by professor)
    public boolean removeFromWaitlist(int waitlistId) {
        WaitlistEntry entry = waitlistDAO.getWaitlistEntryById(waitlistId);
        if (entry == null) {
            return false;
        }

        Appointment appointment = appointmentDAO.getAppointmentByStudentAndSlot(
            entry.getStudent().getUserId(), entry.getSlot().getSlotID());
        if (appointment != null) {
            appointmentDAO.updateAppointmentStatus(
                appointment.getAppointmentId(), AppointmentStatus.CANCELLED);
        }

        return waitlistDAO.removeFromWaitlist(waitlistId);
    }

    // Handle appointment cancellation: if waitlisted, remove from waitlist
    public void handleAppointmentCancellation(int appointmentId) {
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment != null && appointment.getStatus() == AppointmentStatus.WAITLISTED) {
            waitlistDAO.removeByStudentAndSlot(
                appointment.getStudentId(), appointment.getSlotId());
        }
    }

    // Promote highest priority student from waitlist to approved
    public boolean promoteFromWaitlist(int slotId) {
        try {
            WaitlistEntry highest = waitlistDAO.getHighestPriorityStudent(slotId);
            if (highest == null) {
                System.out.println("PROMOTE: no students on waitlist for slot " + slotId);
                return false;
            }

            int studentId = highest.getStudent().getUserId();
            int waitlistId = highest.getWaitlistId();
            System.out.println("PROMOTE: student=" + studentId + " waitlistId=" + waitlistId);

            Appointment appointment = appointmentDAO
                .getAppointmentByStudentAndSlot(studentId, slotId);
            System.out.println("PROMOTE: appointment=" + (appointment != null
                ? "ID=" + appointment.getAppointmentId() + " status=" + appointment.getStatus()
                : "null"));

            if (appointment != null) {
                boolean updated = appointmentDAO.updateAppointmentStatus(
                    appointment.getAppointmentId(), AppointmentStatus.APPROVED);
                System.out.println("PROMOTE: updateAppointmentStatus=" + updated);
                if (!updated) {
                    System.out.println("PROMOTE FAILED: could not approve appointment");
                    return false;
                }
            }

            boolean removed = waitlistDAO.removeFromWaitlist(waitlistId);
            System.out.println("PROMOTE: removeFromWaitlist=" + removed);
            if (!removed) {
                System.out.println("PROMOTE FAILED: could not remove from waitlist");
                return false;
            }

            System.out.println("PROMOTE: success");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
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