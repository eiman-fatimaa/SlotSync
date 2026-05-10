/**
package main.service;
import enums.AppointmentStatus;
import model.Appointment;
import model.Professor;
import model.Student;

public class NotificationService {
    
    private EmailService emailService = new EmailService();

    /**
     * When student books an appointment
     */
    /* public void notifyAppointmentBooked(Appointment appt, Student student, Professor prof) {
        String studentName = student.getFirstName() + " " + student.getLastName();
        String profName = prof.getFirstName() + " " + prof.getLastName();
        
        // Email to PROFESSOR
        String profSubject = "New Appointment Request - " + studentName;
        String profBody = String.format("""
            <h2>New Appointment Request</h2>
            <p><strong>Student:</strong> %s</p>
            <p><strong>Email:</strong> %s</p>
            <p><strong>Reason:</strong> %s</p>
            <p><strong>Note:</strong> %s</p>
            <p><strong>Date:</strong> %s</p>
            <p><strong>Time:</strong> %s - %s</p>
            <p><em>Status: PENDING - Please approve or reject</em></p>
            """, studentName, student.getEmail(), appt.getReason(), 
            appt.getNote() != null ? appt.getNote() : "No additional notes",
            appt.getSlotDate(), appt.getSlotStartTime(), appt.getSlotEndTime());
        
        emailService.sendEmail(prof.getEmail(), profSubject, profBody);
        
        // Email to STUDENT
        String studentSubject = "Appointment Booked - Awaiting Approval";
        String studentBody = String.format("""
            <h2>Appointment Booking Confirmation</h2>
            <p>Dear %s,</p>
            <p>Your appointment request with <strong>%s</strong> has been submitted.</p>
            <p><strong>Details:</strong></p>
            <ul>
                <li>Reason: %s</li>
                <li>Date: %s</li>
                <li>Time: %s - %s</li>
            </ul>
            <p><em>Your appointment is currently <strong>PENDING</strong> approval from the professor.</em></p>
            """, studentName, profName, appt.getReason(),
            appt.getSlotDate(), appt.getSlotStartTime(), appt.getSlotEndTime());
        
        emailService.sendEmail(student.getEmail(), studentSubject, studentBody);
    }

    /**
     * When professor APPROVES appointment
     */
    /* public void notifyAppointmentApproved(Appointment appt, Student student, Professor prof) {
        String studentName = student.getFirstName() + " " + student.getLastName();
        String profName = prof.getFirstName() + " " + prof.getLastName();
        
        // Email to STUDENT
        String studentSubject = "✓ Appointment Approved - " + appt.getReason();
        String studentBody = String.format("""
            <h2>Appointment Approved!</h2>
            <p>Dear %s,</p>
            <p>Your appointment with <strong>%s</strong> has been <strong style="color:green;">APPROVED</strong>.</p>
            <p><strong>Confirmed Details:</strong></p>
            <ul>
                <li>Professor: %s</li>
                <li>Date: %s</li>
                <li>Time: %s - %s</li>
                <li>Reason: %s</li>
            </ul>
            <p>See you then!</p>
            """, studentName, profName, profName, 
            appt.getSlotDate(), appt.getSlotStartTime(), appt.getSlotEndTime(),
            appt.getReason());
        
        emailService.sendEmail(student.getEmail(), studentSubject, studentBody);
        
        // Email to PROFESSOR
        String profSubject = "Appointment Approved - " + studentName;
        String profBody = String.format("""
            <h2>Appointment Confirmed</h2>
            <p>You have approved the appointment with <strong>%s</strong>.</p>
            <p><strong>Details:</strong></p>
            <ul>
                <li>Date: %s</li>
                <li>Time: %s - %s</li>
            </ul>
            """, studentName, appt.getSlotDate(), 
            appt.getSlotStartTime(), appt.getSlotEndTime());
        
        emailService.sendEmail(prof.getEmail(), profSubject, profBody);
    }

    /**
     * When professor REJECTS appointment
     */
    /*public void notifyAppointmentRejected(Appointment appt, Student student, 
                                         Professor prof, String rejectionReason) {
        String studentName = student.getFirstName() + " " + student.getLastName();
        
        String subject = "✗ Appointment Request Rejected";
        String body = String.format("""
            <h2>Appointment Request Rejected</h2>
            <p>Dear %s,</p>
            <p>Your appointment request with <strong>%s</strong> has been <strong style="color:red;">REJECTED</strong>.</p>
            <p><strong>Reason:</strong> %s</p>
            <p>Please try booking another available slot.</p>
            """, studentName, prof.getFirstName() + " " + prof.getLastName(), rejectionReason);
        
        emailService.sendEmail(student.getEmail(), subject, body);
    }

    /**
     * When appointment is CANCELLED
     */
    /*public void notifyAppointmentCancelled(Appointment appt, Student student, 
                                          Professor prof, String cancelledBy) {
        String studentName = student.getFirstName() + " " + student.getLastName();
        String profName = prof.getFirstName() + " " + prof.getLastName();
        
        // If it was APPROVED, notify both
        if (appt.getStatus() == AppointmentStatus.APPROVED) {
            String studentSubject = "Appointment Cancelled - " + appt.getSlotDate();
            String studentBody = String.format("""
                <h2>Appointment Cancelled</h2>
                <p>Dear %s,</p>
                <p>Your appointment with <strong>%s</strong> on <strong>%s</strong> has been cancelled.</p>
                <p><em>Cancelled by: %s</em></p>
                <p>You can book another slot if needed.</p>
                """, studentName, profName, appt.getSlotDate(), cancelledBy);
            
            emailService.sendEmail(student.getEmail(), studentSubject, studentBody);
            
            String profSubject = "Appointment Cancelled - " + studentName;
            String profBody = String.format("""
                <h2>Appointment Cancelled</h2>
                <p>Your appointment with <strong>%s</strong> on <strong>%s</strong> has been cancelled.</p>
                <p><em>Cancelled by: %s</em></p>
                """, studentName, appt.getSlotDate(), cancelledBy);
            
            emailService.sendEmail(prof.getEmail(), profSubject, profBody);
        } 
        else {
            String profSubject = "Appointment Request Cancelled - " + studentName;
            String profBody = String.format("""
                <h2>Appointment Request Cancelled</h2>
                <p>The appointment request from <strong>%s</strong> for <strong>%s</strong> has been cancelled.</p>
                <p><em>Cancelled by: %s</em></p>
                """, studentName, appt.getSlotDate(), cancelledBy);
            
            emailService.sendEmail(prof.getEmail(), profSubject, profBody);
        }
    }

    /**
     * When student is WAITLISTED
     */
    /*public void notifyAppointmentWaitlisted(Appointment appt, Student student, Professor prof) {
        String studentName = student.getFirstName() + " " + student.getLastName();
        String profName = prof.getFirstName() + " " + prof.getLastName();
        
        String subject = "Added to Waitlist - " + appt.getReason();
        String body = String.format("""
            <h2>You've Been Added to the Waitlist</h2>
            <p>Dear %s,</p>
            <p>The slot for <strong>%s</strong> is full. You've been added to the <strong>waitlist</strong>.</p>
            <p><strong>Slot Details:</strong></p>
            <ul>
                <li>Professor: %s</li>
                <li>Date: %s</li>
                <li>Time: %s - %s</li>
            </ul>
            <p>You will be notified if a spot becomes available.</p>
            """, studentName, profName, profName,
            appt.getSlotDate(), appt.getSlotStartTime(), appt.getSlotEndTime());
        
        emailService.sendEmail(student.getEmail(), subject, body);
    }

    /**
     * When WAITLISTED student gets approved (spot opened)
     */
    /*public void notifyWaitlistPromoted(Appointment appt, Student student, Professor prof) {
        String studentName = student.getFirstName() + " " + student.getLastName();
        String profName = prof.getFirstName() + " " + prof.getLastName();
        
        String subject = "✓ Spot Available! Your Appointment is Approved";
        String body = String.format("""
            <h2>Great News!</h2>
            <p>Dear %s,</p>
            <p>A spot has opened up! Your appointment with <strong>%s</strong> has been <strong style="color:green;">APPROVED</strong>.</p>
            <p><strong>Confirmed Details:</strong></p>
            <ul>
                <li>Professor: %s</li>
                <li>Date: %s</li>
                <li>Time: %s - %s</li>
            </ul>
            <p>See you then!</p>
            """, studentName, profName, profName,
            appt.getSlotDate(), appt.getSlotStartTime(), appt.getSlotEndTime());
        
        emailService.sendEmail(student.getEmail(), subject, body);
    }
}

**/