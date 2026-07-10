package ir.ac.kntu.reservation;

import java.util.ArrayList;
import java.util.List;

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
        if (queue.isEmpty()) {
            return null;
        }
        return queue.get(0);
    }

    public List<Reservation> getWaiting() {
        List<Reservation> waiting = new ArrayList<>();
        for (Reservation reservation : queue) {
            if (reservation.isPending()) {
                waiting.add(reservation);
            }
        }
        return waiting;
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
}
