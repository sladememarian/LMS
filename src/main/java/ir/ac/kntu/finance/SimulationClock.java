package ir.ac.kntu.finance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import ir.ac.kntu.util.ClockRepository;

public class SimulationClock {
    private static final int FIRST_DAY = 1;
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("M/d/yyyy");
    private static int currentDay = FIRST_DAY;
    private static LocalDate startDate = LocalDate.now();

    static {
        loadFromDatabase();
    }

    public static int getCurrentDay() {
        return currentDay;
    }

    public static LocalDate getCurrentDate() {
        return startDate.plusDays((long) currentDay - 1);
    }

    public static String formatCurrentDate() {
        return getCurrentDate().format(DISPLAY_FORMAT);
    }

    public static int advanceDay() {
        currentDay++;
        ClockRepository.saveClock(currentDay, startDate);
        return currentDay;
    }

    private static void loadFromDatabase() {
        Map<String, Object> data = ClockRepository.loadClock();
        if (data != null) {
            currentDay = (int) data.get("currentDay");
            startDate = (LocalDate) data.get("startDate");
        } else {
            currentDay = FIRST_DAY;
            startDate = LocalDate.now();
            ClockRepository.saveClock(currentDay, startDate);
        }
    }
}
