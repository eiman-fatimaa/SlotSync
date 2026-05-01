package service;

import dao.AppointmentDAO;
import enums.AppointmentReason;
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
}