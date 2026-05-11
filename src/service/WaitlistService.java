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

//Service layer for managing waitlist operations, including adding/removing students from waitlist, promoting students to approved status when slots become available, and retrieving waitlist details for students and time slots
public class WaitlistService {
    private WaitlistDAO waitlistDAO;        //DAO for interacting with waitlist-related database operations
    private AppointmentDAO appointmentDAO;  //DAO for interacting with appointment-related database operations, used to update appointment status when students are waitlisted or promoted
    private UserDAO userDAO;            //DAO for interacting with user-related database operations, used to retrieve student details when creating waitlist entries/promoting students
    private TimeSlotDAO timeSlotDAO;    //DAO for interacting with time slot-related database operations, used to retrieve time slot details when creating waitlist entries/promoting students

    public WaitlistService() {          //Initialize DAOs in the constructor
        this.waitlistDAO = new WaitlistDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.userDAO = new UserDAO();
        this.timeSlotDAO = new TimeSlotDAO();
    }

    //add student to waitlist (called by professor to waitlist a pending appointment)
    public boolean addToWaitlist(int appointmentId) {
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);     //retrieve the appointment details
        if (appointment == null || appointment.getStatus() != AppointmentStatus.PENDING) {      //only allow waitlisting if the appointment exists and is currently pending, otherwise return false
            return false;
        }

        Student student = (Student) userDAO.getUserById(appointment.getStudentId());        //retrieve the student details using the studentId from the appointment
        TimeSlot slot = timeSlotDAO.getSlotById(appointment.getSlotId());       //retrieve the time slot details using the slotId from the appointment

        if (student == null || slot == null) {          //if either the student or time slot details cannot be retrieved, return false as we cannot create a valid waitlist entry without this information
            return false;
        }

        int priorityScore = PriorityCalculator.calculatePriorityScore(appointment, student);        //calculate the priority score for the waitlist entry based on the defined criteria using the appointment and student details

        boolean statusUpdated = appointmentDAO.updateAppointmentStatus(     //update the appointment status to WAITLISTED in the database to reflect that the student has been waitlisted for this appointment
            appointmentId, AppointmentStatus.WAITLISTED);
        if (!statusUpdated) {
            return false;       //return false if there was an issue updating 
        }

        WaitlistEntry entry = new WaitlistEntry(            //create a new WaitlistEntry object 
            0,
            student,
            slot,
            priorityScore,
            LocalDateTime.now()                             //set the joinedAt timestamp to the current time
        );

        return waitlistDAO.addToWaitlist(entry);            //return true if the waitlist entry was successfull
    }

    //Remove student from waitlist (by professor)
    public boolean removeFromWaitlist(int waitlistId) {
        WaitlistEntry entry = waitlistDAO.getWaitlistEntryById(waitlistId);
        if (entry == null) {        //if the waitlist entry cannot be retrieved using the provided waitlistId 
            return false;
        }

        Appointment appointment = appointmentDAO.getAppointmentByStudentAndSlot(        //retrieve the appointment details 
            entry.getStudent().getUserId(), entry.getSlot().getSlotID());
        if (appointment != null) {
            appointmentDAO.updateAppointmentStatus(
                appointment.getAppointmentId(), AppointmentStatus.CANCELLED);       //update the appointment status to CANCELLED in the database to reflect that the student has been removed from the waitlist for this appointment
        }

        return waitlistDAO.removeFromWaitlist(waitlistId);      //return true if successful removal from waitlist
    }

    //Handle appointment cancellation: if waitlisted, remove from waitlist
    //used when a student cancels an appointment that is currently waitlisted
    public void handleAppointmentCancellation(int appointmentId) {
        Appointment appointment = appointmentDAO.getAppointmentById(appointmentId);     //retrieve the appointment details
        if (appointment != null && appointment.getStatus() == AppointmentStatus.WAITLISTED) {
            waitlistDAO.removeByStudentAndSlot(
                appointment.getStudentId(), appointment.getSlotId());       //remove the student from the waitlist 
        }
    }

    //Promote highest priority student from waitlist to approved
    public boolean promoteFromWaitlist(int slotId) {
        try {
            WaitlistEntry highest = waitlistDAO.getHighestPriorityStudent(slotId);  //retrieve highest priority waitlisted student
            if (highest == null) {      //if there are no waitlisted students 
                System.out.println("PROMOTE: no students on waitlist for slot " + slotId);
                return false;
            }

            int studentId = highest.getStudent().getUserId();       //get the studentId
            int waitlistId = highest.getWaitlistId();               //get the waitlistId
            System.out.println("PROMOTE: student=" + studentId + " waitlistId=" + waitlistId);

            Appointment appointment = appointmentDAO
                .getAppointmentByStudentAndSlot(studentId, slotId);         //retrieve the appointment details
            System.out.println("PROMOTE: appointment=" + (appointment != null
                ? "ID=" + appointment.getAppointmentId() + " status=" + appointment.getStatus()
                : "null"));         //log the appointment details if found, otherwise log that it is null

            if (appointment != null) {
                boolean updated = appointmentDAO.updateAppointmentStatus(
                    appointment.getAppointmentId(), AppointmentStatus.APPROVED);        //update the appointment status to APPROVED in the database to reflect that the student has been promoted from the waitlist to a booked appointment for this slot
                System.out.println("PROMOTE: updateAppointmentStatus=" + updated);
                if (!updated) {
                    System.out.println("PROMOTE FAILED: could not approve appointment");
                    return false;
                }
            }

            boolean removed = waitlistDAO.removeFromWaitlist(waitlistId);   
            System.out.println("PROMOTE: removeFromWaitlist=" + removed);       //remove the student from the waitlist as they have now been promoted to a booked appointment for this slot, log the result of the removal operation
            if (!removed) {
                System.out.println("PROMOTE FAILED: could not remove from waitlist");
                return false;
            }

            System.out.println("PROMOTE: success");     //log successful promotion
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //Get waitlist for a slot
    public List<WaitlistEntry> getWaitlistBySlot(int slotId) {
        return waitlistDAO.getWaitlistBySlot(slotId);
    }

    //Get waitlist for a student
    public List<WaitlistEntry> getWaitlistByStudent(int studentId) {
        return waitlistDAO.getWaitlistByStudent(studentId);
    }
}