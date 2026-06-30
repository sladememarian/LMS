package ir.ac.kntu.reservation;

public class Reservation {

    private static final String STATUS_PENDING = "PENDING";

    private final String reservationId;
    private final String memberId;
    private final String itemId;
    private final int reservedOnDay;
    private final int expiresOnDay;
    private String status;

    public Reservation(String reservationId, String memberId, String itemId,
            int reservedOnDay, int expiresOnDay) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.itemId = itemId;
        this.reservedOnDay = reservedOnDay;
        this.expiresOnDay = expiresOnDay;
        this.status = STATUS_PENDING;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String newStatus) {
        this.status = newStatus;
    }

    public boolean isExpired(int currentDay) {
        return currentDay > expiresOnDay && STATUS_PENDING.equals(status);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }
}
