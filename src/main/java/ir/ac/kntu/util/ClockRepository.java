package ir.ac.kntu.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

// Persistence for the single-row clock_state table backing
// SimulationClock. Split out of the former monolithic
// DatabaseAccess class as part of the per-domain repository migration.
public final class ClockRepository {

    private ClockRepository() {
    }

    public static void clearClock() {
        Database.executeUpdate("DELETE FROM clock_state");
    }

    public static void saveClock(int currentDay, LocalDate startDate) {
        Database.withPs("MERGE INTO clock_state USING (VALUES (1, ?, ?)) AS s(id, current_day, start_date) ON clock_state.id = s.id WHEN MATCHED THEN UPDATE SET current_day = s.current_day, start_date = s.start_date WHEN NOT MATCHED THEN INSERT (id, current_day, start_date) VALUES (s.id, s.current_day, s.start_date)", ps -> {
            ps.setInt(1, currentDay);
            ps.setString(2, startDate.toString());
            ps.executeUpdate();
        });
    }

    public static Map<String, Object> loadClock() {
        return Database.querySingle("SELECT current_day, start_date FROM clock_state WHERE id=1", rs -> {
            Map<String, Object> result = new HashMap<>();
            result.put("currentDay", rs.getInt("current_day"));
            result.put("startDate", LocalDate.parse(rs.getString("start_date")));
            return result;
        });
    }
}
