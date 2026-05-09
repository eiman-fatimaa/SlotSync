package service;

import dao.AppointmentDAO;
import enums.AppointmentReason;
import enums.AppointmentStatus;
import model.Appointment;
import java.util.List;

public class AppointmentService {

    private AppointmentDAO appointmentDAO = new AppointmentDAO();

    public List<Appointment> getStudentAppointments(int studentId) {
        return appointmentDAO.getAppointmentsByStudent(studentId);
    }

    public List<Object[]> getAvailableSlots() {
        return appointmentDAO.getAvailableSlots();
    }

    public boolean bookAppointment(int studentId, int slotId,
                                    AppointmentReason reason) {
        if (studentId <= 0 || slotId <= 0 || reason == null) {
            System.out.println("Invalid booking details");
            return false;
        }
        return appointmentDAO.bookAppointment(studentId, slotId, reason);
    }

    public boolean cancelAppointment(int appointmentId, int slotId) {
        if (appointmentId <= 0) {
            System.out.println("Invalid appointment ID");
            return false;
        }

        // Get appointment status before cancelling
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment == null) {
            return false;
        }

        AppointmentStatus originalStatus = appointment.getStatus();

        // Cancel the appointment
        boolean cancelled = appointmentDAO.cancelAppointment(appointmentId, slotId);
        if (!cancelled) {
            return false;
        }

        // Handle waitlist logic
        WaitlistService waitlistService = new WaitlistService();
        if (originalStatus == AppointmentStatus.WAITLISTED) {
            waitlistService.handleAppointmentCancellation(appointmentId);
        } else if (originalStatus == AppointmentStatus.APPROVED) {
            waitlistService.promoteFromWaitlist(slotId);
        }

        return true;
    }

    // to get pending appointments for a professor
    public List<Appointment> getPendingAppointmentsForProfessor(int professorId) {
    AppointmentDAO dao = new AppointmentDAO();
    return dao.getPendingAppointmentsForProfessor(professorId);
    }

    // to update the appointment status
    public boolean updateAppointmentStatus(int appointmentId, AppointmentStatus newStatus) {
    AppointmentDAO dao = new AppointmentDAO();
    return dao.updateAppointmentStatus(appointmentId, newStatus);
    }

}