package ir.ac.kntu.reservation;

public class Reservation {

    private static final int DEFAULT_RESERVATION_DAYS = 7;

    private final String reservationId;
    private final String memberId;
    private final String itemId;
    private final int reservedOnDay;
    private int expiresOnDay;
    private ReservationStatus status;

    public Reservation(String reservationId, String memberId, String itemId,
            int reservedOnDay, int expiresOnDay, ReservationStatus status) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.itemId = itemId;
        this.reservedOnDay = reservedOnDay;
        this.expiresOnDay = expiresOnDay;
        this.status = status;
    }

    public Reservation(String reservationId, String memberId, String itemId,
            int reservedOnDay, ReservationStatus status) {
        this(reservationId, memberId, itemId, reservedOnDay,
                reservedOnDay + DEFAULT_RESERVATION_DAYS, status);
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getReservedOnDay() {
        return reservedOnDay;
    }

    public int getExpiresOnDay() {
        return expiresOnDay;
    }

    public void setExpiresOnDay(int newExpiresOnDay) {
        this.expiresOnDay = newExpiresOnDay;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus newStatus) {
        this.status = newStatus;
    }

    public boolean isExpired(int currentDay) {
        return currentDay > expiresOnDay
                && (status == ReservationStatus.WAITING || status == ReservationStatus.ACTIVE);
    }

    public boolean isPending() {
        return status == ReservationStatus.WAITING;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean isTerminal() {
        return status == ReservationStatus.COMPLETED
                || status == ReservationStatus.EXPIRED
                || status == ReservationStatus.CANCELLED;
    }
}
