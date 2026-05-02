package service;

import dao.TimeSlotDAO;
import enums.TimeSlotStatus;
import model.TimeSlot;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TimeSlotService {

    private TimeSlotDAO timeSlotDAO = new TimeSlotDAO();


    // these methods use the corresponding DAO methods to retrieve/update data from the DB

    public TimeSlot getSlotById(int slotId) {
        return timeSlotDAO.getSlotById(slotId);
    }

    public List<TimeSlot> getProfessorUpcomingSlots(int professorId) {
        return timeSlotDAO.getUpcomingSlotsByProfessor(professorId);
    }

    public List<TimeSlot> getProfessorThisWeekSlots(int professorId) {
        return timeSlotDAO.getThisWeekSlots(professorId);
    }

    public List<TimeSlot> getProfessorNextWeekSlots(int professorId) {
        return timeSlotDAO.getNextWeekSlots(professorId);
    }

    public List<TimeSlot> getProfessorPastSlots(int professorId) {
        return timeSlotDAO.getPastSlots(professorId);
    }

    public List<TimeSlot> getSlotsByDate(LocalDate date) {
        return timeSlotDAO.getSlotsByDate(date);
    }

    public List<TimeSlot> getAllUpcomingSlots() {
        return timeSlotDAO.getAllUpcomingSlots();
    }

   

    public List<TimeSlot> getAvailableSlotsForProfessor(int professorId) {
        return getProfessorUpcomingSlots(professorId).stream()
                .filter(this::isSlotBookable)
                .collect(Collectors.toList());
    }

    public boolean blockSlot(int slotId) {
        return timeSlotDAO.blockSlot(slotId);
    }

    public boolean unblockSlot(int slotId) {
        return timeSlotDAO.unblockSlot(slotId);
    }

    public boolean updateSlotStatus(int slotId, TimeSlotStatus status) {
        return timeSlotDAO.updateSlotStatus(slotId, status);
    }

    public boolean isSlotBookable(TimeSlot slot) {
        if (slot == null) {
            return false;
        }
        if (slot.getIsManuallyBlockedByProf()) {
            return false;
        }
        if (slot.getStatus() != TimeSlotStatus.FREE && slot.getStatus() != TimeSlotStatus.PARTIALLY_BOOKED) {
            return false;
        }
        if (slot.getCurrentBookings() >= slot.getMaxCapacity()) {
            return false;
        }
        return !slot.getSlotDate().isBefore(LocalDate.now());
    }

    public boolean canBookSlot(int slotId) {
        return isSlotBookable(getSlotById(slotId));
    }
}

