package ir.ac.kntu.reservation;

import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.util.LibraryItemRepository;
import ir.ac.kntu.util.PersonaRepository;
import ir.ac.kntu.util.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    @BeforeEach
    void setUp() {
        ReservationRepository.clearReservations();
        PersonaRepository.clearPersonas();
        LibraryService.initCatalog();
        ReservationService.reset();
        ir.ac.kntu.persona.PersonaService.reset();

        // Copies are mutated (borrowed/returned) by these tests and persist
        // in the shared catalog store across test methods within the same
        // run, so restore the seeded counts every time to keep tests
        // order-independent.
        resetCopies("ITEM-001", 5);
        resetCopies("ITEM-003", 3);

        // Register test users
        ir.ac.kntu.persona.PersonaService.registerPersona("stu1@test.com", "Pass123!");
        ir.ac.kntu.persona.PersonaService.registerPersona("stu2@test.com", "Pass123!");
    }

    private void resetCopies(String itemId, int totalCopies) {
        LibraryItem item = LibraryService.getItemById(itemId);
        item.setTotalCopies(totalCopies);
        item.setAvailableCopies(totalCopies);
        LibraryItemRepository.insertLibraryItem(item);
        LibraryService.initCatalog();
    }

    @Test
    void testReserveItemAvailable() {
        ir.ac.kntu.persona.Persona persona = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com");
        String memberId = persona.getMemberId();
        String itemId = "ITEM-001"; // Clean Code, 5 copies
        int day = SimulationClock.getCurrentDay();

        Reservation reservation = ReservationService.reserve(memberId, itemId, day);
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
        assertEquals(1, ReservationService.getQueue(itemId).size());
        assertEquals(ReservationStatus.ACTIVE, ReservationService.getQueue(itemId).getAll().get(0).getStatus());
    }

    @Test
    void testReserveItemUnavailableQueues() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";
        
        // Take all copies
        for (int i = 0; i < 5; i++) {
            LibraryService.executeBorrow(itemId);
        }

        int day = SimulationClock.getCurrentDay();
        ReservationService.reserve(memberId1, itemId, day);
        Reservation reservation = ReservationService.reserve(memberId2, itemId, day);

        assertEquals(ReservationStatus.WAITING, reservation.getStatus());
        assertEquals(2, ReservationService.getQueue(itemId).size());
        assertEquals(ReservationStatus.WAITING, ReservationService.getQueue(itemId).getAll().get(1).getStatus());
    }

    @Test
    void testReservationExpiration() {
        String memberId = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String itemId = "ITEM-001";
        int day = SimulationClock.getCurrentDay();

        ReservationService.reserve(memberId, itemId, day);
        Reservation res = ReservationService.getQueue(itemId).getAll().get(0);

        // Expire after 7 days (activation period)
        ReservationService.expireReservations(day + 8);

        assertEquals(ReservationStatus.EXPIRED, res.getStatus());
    }

    @Test
    void testReservationActivationOnReturn() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";

        // Take all available copies
        for (int i = 0; i < 5; i++) {
            try {
                LibraryService.executeBorrow(itemId);
            } catch (ir.ac.kntu.exception.BaseException ignored) {
            }
        }

        int day = SimulationClock.getCurrentDay();
        ReservationService.reserve(memberId1, itemId, day);
        ReservationService.reserve(memberId2, itemId, day);

        // Return one copy
        LibraryService.executeReturn(itemId);
        ReservationService.processReturn(itemId, day);

        assertEquals(ReservationStatus.ACTIVE, ReservationService.getQueue(itemId).getAll().get(0).getStatus());
    }

    // ------------------------------------------------------------------
    // Bug-fix regression tests: #1/#2 queue promotion, restock fulfilment,
    // expiry cascade, and walk-in exclusivity over a held copy.
    // ------------------------------------------------------------------

    @Test
    void testSecondUserPromotedAfterFirstReservationIsFulfilled() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";

        for (int i = 0; i < 5; i++) {
            LibraryService.executeBorrow(itemId);
        }

        int day = SimulationClock.getCurrentDay();
        Reservation res1 = ReservationService.reserve(memberId1, itemId, day);
        Reservation res2 = ReservationService.reserve(memberId2, itemId, day);
        assertEquals(1, ReservationService.getQueuePosition(res1.getReservationId(), itemId));
        assertEquals(2, ReservationService.getQueuePosition(res2.getReservationId(), itemId));

        // One copy comes back -> #1 activates (this used to work).
        LibraryService.executeReturn(itemId);
        assertTrue(ReservationService.processReturn(itemId, day));
        assertEquals(ReservationStatus.ACTIVE, res1.getStatus());
        assertEquals(ReservationStatus.WAITING, res2.getStatus());

        // #1 actually picks up the book.
        LibraryService.executeBorrow(itemId);
        ReservationService.completeReservation(memberId1, itemId);
        assertEquals(ReservationStatus.COMPLETED, res1.getStatus());

        // A second copy comes back -> #2 must now be promoted.
        // Before the fix, peekFirst() kept returning res1 (index 0, no
        // longer pending) forever, so this always returned false.
        LibraryService.executeReturn(itemId);
        assertTrue(ReservationService.processReturn(itemId, day),
                "second copy back should promote the next waiting reservation");
        assertEquals(ReservationStatus.ACTIVE, res2.getStatus());
    }

    @Test
    void testRestockFulfillsWaitingQueue() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";

        for (int i = 0; i < 5; i++) {
            LibraryService.executeBorrow(itemId);
        }

        int day = SimulationClock.getCurrentDay();
        Reservation res1 = ReservationService.reserve(memberId1, itemId, day);
        Reservation res2 = ReservationService.reserve(memberId2, itemId, day);

        // Admin/CallCenter restocks 2 copies (no member "return" involved).
        LibraryService.updateItemQuantityFromCallCenter(itemId, 2);
        ReservationService.fulfillFromQueue(itemId, 2, day);

        assertEquals(ReservationStatus.ACTIVE, res1.getStatus());
        assertEquals(ReservationStatus.ACTIVE, res2.getStatus());
    }

    @Test
    void testExpiredActiveReservationCascadesToNextWaiting() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";

        for (int i = 0; i < 5; i++) {
            LibraryService.executeBorrow(itemId);
        }

        int day = SimulationClock.getCurrentDay();
        Reservation res1 = ReservationService.reserve(memberId1, itemId, day);
        Reservation res2 = ReservationService.reserve(memberId2, itemId, day);

        LibraryService.executeReturn(itemId);
        ReservationService.processReturn(itemId, day);
        assertEquals(ReservationStatus.ACTIVE, res1.getStatus());

        // #1 never shows up to borrow it; the reservation window passes.
        ReservationService.expireReservations(res1.getExpiresOnDay() + 1);

        assertEquals(ReservationStatus.EXPIRED, res1.getStatus());
        assertEquals(ReservationStatus.ACTIVE, res2.getStatus(),
                "expiring the held reservation should promote #2 automatically");
    }

    @Test
    void testWalkInCannotStealCopyHeldForActiveReservation() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";

        for (int i = 0; i < 5; i++) {
            LibraryService.executeBorrow(itemId);
        }

        int day = SimulationClock.getCurrentDay();
        ReservationService.reserve(memberId1, itemId, day);

        // The one returned copy is earmarked for memberId1.
        LibraryService.executeReturn(itemId);
        ReservationService.processReturn(itemId, day);
        LibraryItem item = LibraryService.getItemById(itemId);
        assertEquals(1, item.getAvailableCopies());

        // An unrelated member must not see this copy as free for a walk-in borrow.
        int walkInForOther = ReservationService.getWalkInAvailableCopies(
                itemId, memberId2, item.getAvailableCopies());
        assertEquals(0, walkInForOther,
                "copy held for member1's active reservation must not be walk-in borrowable by member2");

        // The reservation holder themselves must still be able to take it.
        int walkInForHolder = ReservationService.getWalkInAvailableCopies(
                itemId, memberId1, item.getAvailableCopies());
        assertEquals(1, walkInForHolder,
                "the reservation holder must still be able to borrow their own held copy");
    }

    @Test
    void testWalkInAllowedWhenSurplusCopiesExistBeyondHeldOnes() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";

        for (int i = 0; i < 5; i++) {
            LibraryService.executeBorrow(itemId);
        }

        int day = SimulationClock.getCurrentDay();
        ReservationService.reserve(memberId1, itemId, day);

        // Two copies come back: one gets held for memberId1, one is surplus.
        LibraryService.executeReturn(itemId);
        LibraryService.executeReturn(itemId);
        ReservationService.processReturn(itemId, day);
        LibraryItem item = LibraryService.getItemById(itemId);
        assertEquals(2, item.getAvailableCopies());

        int walkInForOther = ReservationService.getWalkInAvailableCopies(
                itemId, memberId2, item.getAvailableCopies());
        assertEquals(1, walkInForOther,
                "the surplus (non-held) copy should still be walk-in borrowable");
    }

    @Test
    void testMaxReservationLimitIsPerMemberAcrossAllItems() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        ir.ac.kntu.persona.Persona owner =
                ir.ac.kntu.persona.PersonaService.getProfile("admin@system.local");
        ir.ac.kntu.util.SystemSettingsService.updateMaxReservations(owner, 1);
        try {
            ReservationService.reserve(memberId1, "ITEM-001",
                    SimulationClock.getCurrentDay());
            assertThrows(ir.ac.kntu.exception.ConflictException.class,
                    () -> ReservationService.reserve(memberId1, "ITEM-003",
                            SimulationClock.getCurrentDay()),
                    "system-wide max reservations must cap a member across different items too");
        } finally {
            ir.ac.kntu.util.SystemSettingsService.updateMaxReservations(
                    owner, ir.ac.kntu.util.SystemSettings.DEFAULT_MAX_RESERVATIONS);
        }
    }
}
