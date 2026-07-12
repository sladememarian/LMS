package ir.ac.kntu.util;

import ir.ac.kntu.exception.AuthorizationException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;

/**
 * Lets the Owner/Admin move configurable library values (borrow days, fine
 * rate, reservation days, max reservations) into the database instead of
 * being hardcoded, per Step 11.
 */
public final class SystemSettingsService {

    private static int borrowDays = SystemSettings.DEFAULT_BORROW_DAYS;
    private static int fineRate = SystemSettings.DEFAULT_FINE_RATE;
    private static int reservationDays = SystemSettings.DEFAULT_RESERVATION_DAYS;
    private static int maxReservations = SystemSettings.DEFAULT_MAX_RESERVATIONS;

    static {
        load();
    }

    private SystemSettingsService() {
    }

    public static void reset() {
        load();
    }

    private static void load() {
        java.util.Map<String, String> stored = SystemSettingsRepository.getAll();
        borrowDays = readIntOrDefault(stored, SystemSettings.BORROW_DAYS_KEY, SystemSettings.DEFAULT_BORROW_DAYS);
        fineRate = readIntOrDefault(stored, SystemSettings.FINE_RATE_KEY, SystemSettings.DEFAULT_FINE_RATE);
        reservationDays = readIntOrDefault(stored, SystemSettings.RESERVATION_DAYS_KEY,
                SystemSettings.DEFAULT_RESERVATION_DAYS);
        maxReservations = readIntOrDefault(stored, SystemSettings.MAX_RESERVATIONS_KEY,
                SystemSettings.DEFAULT_MAX_RESERVATIONS);
    }

    private static int readIntOrDefault(java.util.Map<String, String> stored, String key, int fallback) {
        String raw = stored.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static SystemSettings getSettings() {
        return new SystemSettings(borrowDays, fineRate, reservationDays, maxReservations);
    }

    public static int getBorrowDays() {
        return borrowDays;
    }

    public static int getFineRate() {
        return fineRate;
    }

    public static int getReservationDays() {
        return reservationDays;
    }

    public static int getMaxReservations() {
        return maxReservations;
    }

    private static void requireOwnerOrAdmin(Persona actor) {
        if (actor == null || actor.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only the Owner or an Admin can change system settings.");
        }
    }

    private static void requirePositive(String label, int value) {
        if (value <= 0) {
            throw new ValidationException(label + " must be a positive number.");
        }
    }

    public static void updateBorrowDays(Persona actor, int value) {
        requireOwnerOrAdmin(actor);
        requirePositive("Borrow days", value);
        borrowDays = value;
        SystemSettingsRepository.save(SystemSettings.BORROW_DAYS_KEY, String.valueOf(value));
    }

    public static void updateFineRate(Persona actor, int value) {
        requireOwnerOrAdmin(actor);
        requirePositive("Fine rate", value);
        fineRate = value;
        SystemSettingsRepository.save(SystemSettings.FINE_RATE_KEY, String.valueOf(value));
    }

    public static void updateReservationDays(Persona actor, int value) {
        requireOwnerOrAdmin(actor);
        requirePositive("Reservation days", value);
        reservationDays = value;
        SystemSettingsRepository.save(SystemSettings.RESERVATION_DAYS_KEY, String.valueOf(value));
    }

    public static void updateMaxReservations(Persona actor, int value) {
        requireOwnerOrAdmin(actor);
        requirePositive("Max reservations", value);
        maxReservations = value;
        SystemSettingsRepository.save(SystemSettings.MAX_RESERVATIONS_KEY, String.valueOf(value));
    }
}
