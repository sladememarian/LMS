package ir.ac.kntu.reservation;

import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.util.DatabaseAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    @BeforeEach
    void setUp() {
        DatabaseAccess.clearReservations();
        DatabaseAccess.clearPersonas();
        LibraryService.initCatalog();
        ReservationService.reset();
        ir.ac.kntu.persona.PersonaService.reset();
        
        // Register test users
        ir.ac.kntu.persona.PersonaService.registerPersona("stu1@test.com", "Pass123!");
        ir.ac.kntu.persona.PersonaService.registerPersona("stu2@test.com", "Pass123!");
    }

    @Test
    void testReserveItemAvailable() {
        ir.ac.kntu.persona.Persona persona = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com");
        String memberId = persona.getMemberId();
        String itemId = "ITEM-001"; // Clean Code, 5 copies
        int day = SimulationClock.getCurrentDay();

        String result = ReservationService.reserve(memberId, itemId, day);
        assertTrue(result.contains("activated"), "Result was: " + result);
        assertEquals(1, ReservationService.getQueue(itemId).size());
        assertEquals(ReservationStatus.ACTIVE, ReservationService.getQueue(itemId).peekFirst().getStatus());
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
        String result = ReservationService.reserve(memberId2, itemId, day);

        assertTrue(result.contains("queue"));
        assertEquals(2, ReservationService.getQueue(itemId).size());
        assertEquals(ReservationStatus.WAITING, ReservationService.getQueue(itemId).getAll().get(1).getStatus());
    }

    @Test
    void testReservationExpiration() {
        String memberId = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String itemId = "ITEM-001";
        int day = SimulationClock.getCurrentDay();

        ReservationService.reserve(memberId, itemId, day);
        Reservation res = ReservationService.getQueue(itemId).peekFirst();

        // Expire after 7 days (activation period)
        ReservationService.expireReservations(day + 8);

        assertEquals(ReservationStatus.EXPIRED, res.getStatus());
    }

    @Test
    void testReservationActivationOnReturn() {
        String memberId1 = ir.ac.kntu.persona.PersonaService.getProfile("stu1@test.com").getMemberId();
        String memberId2 = ir.ac.kntu.persona.PersonaService.getProfile("stu2@test.com").getMemberId();
        String itemId = "ITEM-001";

        // Take all copies
        for (int i = 0; i < 5; i++) {
            LibraryService.executeBorrow(itemId);
        }

        int day = SimulationClock.getCurrentDay();
        ReservationService.reserve(memberId1, itemId, day);
        ReservationService.reserve(memberId2, itemId, day);

        // Return one copy
        LibraryService.executeReturn(itemId);
        ReservationService.processReturn(itemId, day);

        assertEquals(ReservationStatus.ACTIVE, ReservationService.getQueue(itemId).getAll().get(0).getStatus());
    }
}
