package ir.ac.kntu.finance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import ir.ac.kntu.util.EnvConfig;

/**
 * The simulated calendar of the whole system. The Admin is the "god of time":
 * advancing the clock moves every running instance to the next simulated day
 * and real calendar date. Both the integer day counter (used by
 * {@link LoanService} for due-day arithmetic) and the base calendar date are
 * persisted to a local XOR-encrypted file and re-read on every access, so two
 * simultaneously running instances always agree on "today". The base date is
 * set to the real current date on first creation and never changes; subsequent
 * advances shift the displayed date forward one calendar day at a time.
 */
public class SimulationClock {

    private static final String FILE_PATH = "clock_secure.json";
    private static final String KEY_DAY = "day";
    private static final String KEY_BASE = "base";
    private static final String FIELD_SEP = "\": \"";
    private static final int FIRST_DAY = 1;
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("M/d/yyyy");
    private static int currentDay = FIRST_DAY;
    private static LocalDate startDate = LocalDate.now();

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    public static int getCurrentDay() {
        load();
        return currentDay;
    }

    public static LocalDate getCurrentDate() {
        load();
        return startDate.plusDays((long) currentDay - 1);
    }

    public static String formatCurrentDate() {
        return getCurrentDate().format(DISPLAY_FORMAT);
    }

    public static int advanceDay() {
        load();
        currentDay++;
        save();
        return currentDay;
    }

    private static void save() {
        String raw = "{\n  \"" + KEY_DAY + FIELD_SEP + currentDay
                + "\",\n  \"" + KEY_BASE + FIELD_SEP + startDate.toString() + "\"\n}";
        byte[] keyBytes = getEncryptionKey();
        byte[] rawBytes = raw.getBytes();
        byte[] enc = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            enc[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(enc);
        } catch (IOException ex) {
            System.err.println("Error saving simulation clock: " + ex.getMessage());
        }
    }

    private static void load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            currentDay = FIRST_DAY;
            startDate = LocalDate.now();
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] enc = fis.readAllBytes();
            byte[] keyBytes = getEncryptionKey();
            byte[] dec = new byte[enc.length];
            for (int i = 0; i < enc.length; i++) {
                dec[i] = (byte) (enc[i] ^ keyBytes[i % keyBytes.length]);
            }
            parseClockData(new String(dec));
        } catch (IOException ex) {
            System.err.println("Error loading simulation clock: " + ex.getMessage());
        }
    }

    private static void parseClockData(String raw) {
        currentDay = parseIntField(raw, KEY_DAY);
        String baseStr = parseStrField(raw, KEY_BASE);
        startDate = baseStr == null ? LocalDate.now() : LocalDate.parse(baseStr);
    }

    private static int parseIntField(String raw, String key) {
        String value = parseStrField(raw, key);
        return value == null || value.isEmpty() ? FIRST_DAY : Integer.parseInt(value);
    }

    private static String parseStrField(String raw, String key) {
        String token = "\"" + key + FIELD_SEP;
        int start = raw.indexOf(token);
        if (start == -1) {
            return null;
        }
        start += token.length();
        return raw.substring(start, raw.indexOf("\"", start));
    }
}
