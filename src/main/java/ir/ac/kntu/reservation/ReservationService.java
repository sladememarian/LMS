package ir.ac.kntu.reservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import ir.ac.kntu.exception.ConflictException;
import ir.ac.kntu.exception.NotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.util.ReservationRepository;
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
        ALL_RESERVATIONS.addAll(ReservationRepository.getAllReservations());
        for (Reservation reservation : ALL_RESERVATIONS) {
            QUEUES.computeIfAbsent(reservation.getItemId(),
                    ReservationQueue::new).enqueue(reservation);
        }
    }

    private static void syncReservation(Reservation reservation) {
        ReservationRepository.insertReservation(reservation);
    }

    public static Reservation reserve(String memberId, String itemId, int currentDay) {
        LibraryItem item = validateReservationRequest(memberId, itemId);

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
            return reservation;
        }

        Reservation reservation = new Reservation(
                generateId(), memberId, itemId,
                currentDay, ReservationStatus.WAITING);
        ALL_RESERVATIONS.add(reservation);
        queue.enqueue(reservation);
        syncReservation(reservation);
        return reservation;
    }

    private static LibraryItem validateReservationRequest(String memberId, String itemId) {
        LibraryItem item = LibraryService.getItemById(itemId);
        if (item == null) {
            throw new NotFoundException("Item not found: " + itemId);
        }
        if (!item.canReserve()) {
            throw new ValidationException("This item type cannot be reserved.");
        }
        if (hasActiveReservation(memberId, itemId)) {
            throw new ConflictException("You already have a reservation for this item.");
        }
        int activeCount = getActiveReservationCount(memberId);
        if (activeCount >= getReservationLimit(memberId)) {
            throw new ConflictException("Reservation limit reached for your role.");
        }
        return item;
    }

    public static void cancel(String reservationId) {
        for (Reservation reservation : ALL_RESERVATIONS) {
            if (reservation.getReservationId().equals(reservationId)
                    && !reservation.isTerminal()) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                syncReservation(reservation);
                return;
            }
        }
        throw new NotFoundException("Reservation not found or already completed: " + reservationId);
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

    public static boolean hasActiveReservation(String memberId,
            String itemId) {
        return ALL_RESERVATIONS.stream()
                .anyMatch(r -> r.getMemberId().equals(memberId)
                        && r.getItemId().equals(itemId)
                        && !r.isTerminal());
    }

    public static List<Reservation> getMemberReservations(
            String memberId) {
        return ALL_RESERVATIONS.stream()
                .filter(r -> r.getMemberId().equals(memberId)
                        && !r.isTerminal())
                .collect(Collectors.toList());
    }

    public static int getActiveReservationCount(String memberId) {
        return getMemberReservations(memberId).size();
    }

    public static ReservationQueue getQueue(String itemId) {
        return QUEUES.get(itemId);
    }

    public static int getQueuePosition(String reservationId,
            String itemId) {
        ReservationQueue queue = QUEUES.get(itemId);
        if (queue == null) {
            return -1;
        }
        for (Reservation r : queue.getAll()) {
            if (r.getReservationId().equals(reservationId)) {
                return queue.getPosition(r);
            }
        }
        return -1;
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
