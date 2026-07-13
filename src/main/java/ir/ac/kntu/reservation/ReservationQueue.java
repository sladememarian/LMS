package ir.ac.kntu.reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationQueue {

    private final String itemId;
    private final List<Reservation> queue;

    public ReservationQueue(String itemId) {
        this.itemId = itemId;
        this.queue = new ArrayList<>();
    }

    public String getItemId() {
        return itemId;
    }

    public void enqueue(Reservation reservation) {
        queue.add(reservation);
    }

    public boolean remove(Reservation reservation) {
        return queue.remove(reservation);
    }

    public Reservation peekFirst() {
        for (Reservation reservation : queue) {
            if (reservation.isPending()) {
                return reservation;
            }
        }
        return null;
    }

    public List<Reservation> getWaiting() {
        return queue.stream()
                .filter(Reservation::isPending)
                .collect(Collectors.toList());
    }

    public List<Reservation> getAll() {
        return new ArrayList<>(queue);
    }

    public int waitingCount() {
        return getWaiting().size();
    }

    public int size() {
        return queue.size();
    }

    public boolean hasWaiting() {
        return !getWaiting().isEmpty();
    }

    public int getPosition(Reservation reservation) {
        int pos = 1;
        for (Reservation r : queue) {
            if (r.isPending()) {
                if (r.getReservationId().equals(
                        reservation.getReservationId())) {
                    return pos;
                }
                pos++;
            }
        }
        return -1;
    }
}
