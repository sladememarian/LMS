package ir.ac.kntu.interfaces;

public interface Reservable {

    void reserve(String memberId, int expiresOnDay);

    void cancelReservation(String memberId);

    boolean hasActiveReservation(String memberId);
}
