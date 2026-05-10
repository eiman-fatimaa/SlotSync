package service;

import java.util.List;

import dao.AppointmentDAO;
import enums.AppointmentReason;
import enums.AppointmentStatus;
import model.Appointment;
//a service layer for appt operations - baiscally sits btw views and appouintment dao 
public class AppointmentService {
    //dao instance will be used for all db calls
    private AppointmentDAO appointmentDAO = new AppointmentDAO();
    //returns all appt for a single student
    public List<Appointment> getStudentAppointments(int studentId) {
        return appointmentDAO.getAppointmentsByStudent(studentId);
    }
    //returns all free/partially booked future slots for a given student
    public List<Object[]> getAvailableSlots() {
        return appointmentDAO.getAvailableSlots();
    }
    //first validates the input and then books tsudent appt
    public boolean bookAppointment(int studentId, int slotId,
                                    AppointmentReason reason) {
        if (studentId <= 0 || slotId <= 0 || reason == null) {
            System.out.println("Invalid booking details");
            return false;
        }
        return appointmentDAO.bookAppointment(studentId, slotId, reason);
    }
    //cancels an appt and handles waitlist promotion if needed
    public boolean cancelAppointment(int appointmentId, int slotId) {
        if (appointmentId <= 0) {
            System.out.println("Invalid appointment ID");
            return false;
        }

        // Get appointment status before cancelling - checks current status of appt
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment == null) {
            return false;
        }

        AppointmentStatus originalStatus = appointment.getStatus();

        // Cancel the appointment in db
        boolean cancelled = appointmentDAO.cancelAppointment(appointmentId, slotId);
        if (!cancelled) {
            return false;
        }

        // Handle waitlist logic based on orginal status of appt
        WaitlistService waitlistService = new WaitlistService();
        if (originalStatus == AppointmentStatus.WAITLISTED) {
            //removed from waitlist table
            waitlistService.handleAppointmentCancellation(appointmentId);
        } else if (originalStatus == AppointmentStatus.APPROVED) {
            //a spot freed up — promote top waitlisted student if any
            waitlistService.promoteFromWaitlist(slotId);
        }

        return true;
    }

    //returns all pending appointments for a professor
    public List<Appointment> getPendingAppointmentsForProfessor(int professorId) {
        AppointmentDAO dao = new AppointmentDAO();
        return dao.getPendingAppointmentsForProfessor(professorId);
    }

    //updates the appointment status - used by prof to approve/reject/waitlist
    public boolean updateAppointmentStatus(int appointmentId, AppointmentStatus newStatus) {
        AppointmentDAO dao = new AppointmentDAO();
        return dao.updateAppointmentStatus(appointmentId, newStatus);
    }

}