package ir.ac.kntu.reservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.util.DatabaseAccess;
import ir.ac.kntu.util.SystemSettingsService;

public class ReservationService {

    private static final List<Reservation> ALL_RESERVATIONS = new ArrayList<>();
    private static final Map<String, ReservationQueue> QUEUES = new HashMap<>();

    static {
        loadFromDatabase();
    }

    public static void reset() {
        loadFromDatabase();
    }

    private static void loadFromDatabase() {
        ALL_RESERVATIONS.clear();
        QUEUES.clear();
        ALL_RESERVATIONS.addAll(DatabaseAccess.getAllReservations());
        for (Reservation reservation : ALL_RESERVATIONS) {
            QUEUES.computeIfAbsent(reservation.getItemId(),
                    ReservationQueue::new).enqueue(reservation);
        }
    }

    private static void syncReservation(Reservation reservation) {
        DatabaseAccess.insertReservation(reservation);
    }

    public static String reserve(String memberId, String itemId, int currentDay) {
        LibraryItem item = LibraryService.getItemById(itemId);
        if (item == null) {
            return "Item not found.";
        }
        if (!item.canReserve()) {
            return "This item type cannot be reserved.";
        }
        if (hasActiveReservation(memberId, itemId)) {
            return "You already have a reservation for this item.";
        }
        int activeCount = getActiveReservationCount(memberId);
        if (activeCount >= getReservationLimit(memberId)) {
            return "Reservation limit reached for your role.";
        }

        ReservationQueue queue = QUEUES.computeIfAbsent(itemId,
                ReservationQueue::new);

        if (item.getAvailableCopies() > 0) {
            Reservation reservation = new Reservation(
                    generateId(), memberId, itemId,
                    currentDay, currentDay + SystemSettingsService.getReservationDays(),
                    ReservationStatus.ACTIVE);
            ALL_RESERVATIONS.add(reservation);
            queue.enqueue(reservation);
            syncReservation(reservation);
            return "Reservation activated. Pick up by day "
                    + reservation.getExpiresOnDay() + ".";
        }

        Reservation reservation = new Reservation(
                generateId(), memberId, itemId,
                currentDay, ReservationStatus.WAITING);
        ALL_RESERVATIONS.add(reservation);
        queue.enqueue(reservation);
        syncReservation(reservation);
        return "Item unavailable. You are #" + queue.waitingCount()
                + " in the queue.";
    }

    public static boolean cancel(String reservationId) {
        for (Reservation reservation : ALL_RESERVATIONS) {
            if (reservation.getReservationId().equals(reservationId)
                    && !reservation.isTerminal()) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                syncReservation(reservation);
                return true;
            }
        }
        return false;
    }

    public static boolean processReturn(String itemId, int currentDay) {
        ReservationQueue queue = QUEUES.get(itemId);
        if (queue == null || !queue.hasWaiting()) {
            return false;
        }
        Reservation first = queue.peekFirst();
        if (first == null || !first.isPending()) {
            return false;
        }
        first.setStatus(ReservationStatus.ACTIVE);
        syncReservation(first);
        System.out.println("[Reservation]: Activated reservation "
                + first.getReservationId() + " for member "
                + first.getMemberId() + ". Pick up by day "
                + first.getExpiresOnDay() + ".");
        return true;
    }

    public static void expireReservations(int currentDay) {
        for (Reservation reservation : ALL_RESERVATIONS) {
            if (reservation.isExpired(currentDay)) {
                reservation.setStatus(ReservationStatus.EXPIRED);
                syncReservation(reservation);
                System.out.println("[Reservation]: Expired reservation "
                        + reservation.getReservationId() + " for item "
                        + reservation.getItemId() + ".");
            }
        }
    }

    public static void completeReservation(String memberId, String itemId) {
        for (Reservation reservation : ALL_RESERVATIONS) {
            if (reservation.getMemberId().equals(memberId)
                    && reservation.getItemId().equals(itemId)
                    && reservation.isActive()) {
                reservation.setStatus(ReservationStatus.COMPLETED);
                syncReservation(reservation);
                return;
            }
        }
    }

    public static boolean hasActiveReservation(String memberId, String itemId) {
        for (Reservation reservation : ALL_RESERVATIONS) {
            if (reservation.getMemberId().equals(memberId)
                    && reservation.getItemId().equals(itemId)
                    && !reservation.isTerminal()) {
                return true;
            }
        }
        return false;
    }

    public static List<Reservation> getMemberReservations(String memberId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation reservation : ALL_RESERVATIONS) {
            if (reservation.getMemberId().equals(memberId)
                    && !reservation.isTerminal()) {
                result.add(reservation);
            }
        }
        return result;
    }

    public static int getActiveReservationCount(String memberId) {
        return getMemberReservations(memberId).size();
    }

    public static ReservationQueue getQueue(String itemId) {
        return QUEUES.get(itemId);
    }

    public static List<Reservation> getAllReservations() {
        return new ArrayList<>(ALL_RESERVATIONS);
    }

    private static int getReservationLimit(String memberId) {
        ir.ac.kntu.persona.Persona persona =
                ir.ac.kntu.persona.PersonaService
                        .getProfileByMemberId(memberId);
        if (persona == null) {
            return 0;
        }
        int roleLimit = persona.getUserProfile().reservationLimit();
        return Math.min(roleLimit, SystemSettingsService.getMaxReservations());
    }

    private static String generateId() {
        return "RES-" + UUID.randomUUID().toString().substring(0, 8)
                .toUpperCase();
    }
}
