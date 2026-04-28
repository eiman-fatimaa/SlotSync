package dao;

import java.sql.*;
import java.time.LocalTime;
import java.util.*;

import model.Timetable;
import model.TimetableEntry;

public class TimetableDAO {

    public List<TimetableEntry> getTimetableByProfessor(int professorId) {

        List<TimetableEntry> list = new ArrayList<>();
        Connection conn = DBConnection.getConnection();

        try {
            String query = """
                SELECT t.day, t.start_time, t.end_time, t.is_busy
                FROM timetable t
                JOIN professor_timetable pt ON t.timetable_id = pt.timetable_id
                WHERE pt.professor_id = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, professorId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                TimetableEntry entry = new TimetableEntry(
                        rs.getString("day"),
                        LocalTime.parse(rs.getString("start_time")),
                        LocalTime.parse(rs.getString("end_time")),
                        rs.getBoolean("is_busy")
                );

                list.add(entry);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void saveTimetable(Timetable timetable) {

        Connection conn = DBConnection.getConnection();

        try {
            // 1. Insert into professor_timetable (once)
            String insertPT = "INSERT INTO professor_timetable(professor_id) VALUES (?)";
            PreparedStatement ptStmt = conn.prepareStatement(insertPT, Statement.RETURN_GENERATED_KEYS);
            ptStmt.setInt(1, timetable.getProfessorId());
            ptStmt.executeUpdate();

            ResultSet keys = ptStmt.getGeneratedKeys();
            keys.next();
            int timetableId = keys.getInt(1);

            // 2. Insert each timetable entry
            String insertT = """
                INSERT INTO timetable(timetable_id, day, start_time, end_time, is_busy)
                VALUES (?, ?, ?, ?, ?)
            """;

            for (TimetableEntry entry : timetable.getEntries()) {
                PreparedStatement tStmt = conn.prepareStatement(insertT);

                tStmt.setInt(1, timetableId);
                tStmt.setString(2, entry.getDay());
                tStmt.setString(3, entry.getStartTime().toString());
                tStmt.setString(4, entry.getEndTime().toString());
                tStmt.setBoolean(5, entry.isBusy());

                tStmt.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}