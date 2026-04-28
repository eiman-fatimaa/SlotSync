package util;

import model.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TimetableParser {

    public static Timetable parseJSON(String filePath, int professorId) {
        List<TimetableEntry> entries = new ArrayList<>();

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            JSONArray arr = new JSONArray(content);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                String day = obj.getString("day");
                LocalTime start = LocalTime.parse(obj.getString("start"));
                LocalTime end = LocalTime.parse(obj.getString("end"));
                boolean busy = obj.getBoolean("busy");

                entries.add(new TimetableEntry(day, start, end, busy));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Timetable(professorId, entries);
    }
}