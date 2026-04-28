package model;

import java.util.List;

public class Timetable {
    private int professorId;
    private List<TimetableEntry> entries;

    public Timetable(int professorId, List<TimetableEntry> entries) {
        this.professorId = professorId;
        this.entries = entries;
    }

    public int getProfessorId() { return professorId; }
    public List<TimetableEntry> getEntries() { return entries; }
}