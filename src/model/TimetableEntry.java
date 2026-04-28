package model;

import java.time.LocalTime;

public class TimetableEntry {
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isBusy;

    public TimetableEntry(String day, LocalTime startTime,
                          LocalTime endTime, boolean isBusy) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBusy = isBusy;
    }

    public String getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isBusy() { return isBusy; }
}