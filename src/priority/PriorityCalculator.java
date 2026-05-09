package priority;

import enums.AppointmentReason;
import model.Appointment;
import model.Student;

public class PriorityCalculator {

    public static int calculatePriorityScore(
            Appointment appointment,
            Student student) {

        int reasonScore =
                getReasonScore(
                        appointment.getReason());

        int yearScore =
                getYearScore(
                        student.getYear());

        return reasonScore + yearScore;
    }

    private static int getReasonScore(
            AppointmentReason reason) {

        switch (reason) {

            case CLEARANCE:
                return 120;

            case PAPER_RECHECK:
                return 90;

            case THESIS_FYP:
                return 75;

            case GRADE_APPEAL:
                return 65;

            case ATTENDANCE_SHORTAGE:
                return 55;

            case COURSE_REGISTRATION:
                return 45;

            case RECOMMENDATION_LETTER:
                return 35;

            case GENERAL_ADVICE:
                return 20;

            default:
                return 0;
        }
    }

    private static int getYearScore(
            int year) {

        switch (year) {

            case 4:
                return 40;

            case 3:
                return 30;

            case 2:
                return 20;

            case 1:
                return 10;

            default:
                return 0;
        }
    }
}