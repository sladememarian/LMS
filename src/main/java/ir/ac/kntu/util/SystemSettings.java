package ir.ac.kntu.util;

// Library-wide configurable values (Step 11): how many days an item can be
// borrowed, the daily overdue fine, how many days a reservation stays valid,
// and how many active reservations a member may hold at once. These used to
// be hardcoded constants scattered across the codebase; they now live here
// and are backed by the {@code system_settings} table so the Owner/Admin can
// edit them at runtime.
public final class SystemSettings {

    public static final String BORROW_DAYS_KEY = "borrowDays";
    public static final String FINE_RATE_KEY = "fineRate";
    public static final String RESERVATION_DAYS_KEY = "reservationDays";
    public static final String MAX_RESERVATIONS_KEY = "maxReservations";

    // Defaults are ceilings, not targets: they must be at or above the most
    // generous existing per-item/per-role value (AudioBook/EBook borrow at 21
    // days, Teacher reserves up to 10 items) so installing this feature never
    // silently shrinks a limit the Owner/Admin hasn't touched yet.
    public static final int DEFAULT_BORROW_DAYS = 21;
    public static final int DEFAULT_FINE_RATE = 10_000;
    public static final int DEFAULT_RESERVATION_DAYS = 7;
    public static final int DEFAULT_MAX_RESERVATIONS = 10;

    private final int borrowDays;
    private final int fineRate;
    private final int reservationDays;
    private final int maxReservations;

    public SystemSettings(int borrowDays, int fineRate, int reservationDays, int maxReservations) {
        this.borrowDays = borrowDays;
        this.fineRate = fineRate;
        this.reservationDays = reservationDays;
        this.maxReservations = maxReservations;
    }

    public static SystemSettings defaults() {
        return new SystemSettings(DEFAULT_BORROW_DAYS, DEFAULT_FINE_RATE,
                DEFAULT_RESERVATION_DAYS, DEFAULT_MAX_RESERVATIONS);
    }

    public int getBorrowDays() {
        return borrowDays;
    }

    public int getFineRate() {
        return fineRate;
    }

    public int getReservationDays() {
        return reservationDays;
    }

    public int getMaxReservations() {
        return maxReservations;
    }
}
