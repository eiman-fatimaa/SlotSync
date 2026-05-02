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
        return appointmentDAO.cancelAppointment(appointmentId, slotId);
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