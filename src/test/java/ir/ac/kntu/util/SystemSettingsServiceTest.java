package ir.ac.kntu.util;

import ir.ac.kntu.exception.AuthorizationException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemSettingsServiceTest {

    private Persona owner;
    private Persona guest;

    @BeforeEach
    void setUp() {
        SystemSettingsRepository.clear();
        SystemSettingsService.reset();
        PersonaRepository.clearPersonas();
        PersonaService.reset();
        ReservationRepository.clearReservations();
        ReservationService.reset();
        owner = PersonaService.getProfile("admin@system.local");
        guest = PersonaService.registerPersona("guest@test.com", "pass");
    }

    @Test
    void defaultsApplyBeforeAnyUpdate() {
        assertEquals(SystemSettings.DEFAULT_BORROW_DAYS, SystemSettingsService.getBorrowDays());
        assertEquals(SystemSettings.DEFAULT_FINE_RATE, SystemSettingsService.getFineRate());
        assertEquals(SystemSettings.DEFAULT_RESERVATION_DAYS, SystemSettingsService.getReservationDays());
        assertEquals(SystemSettings.DEFAULT_MAX_RESERVATIONS, SystemSettingsService.getMaxReservations());
    }

    @Test
    void nonAdminCannotUpdateSettings() {
        assertThrows(AuthorizationException.class,
                () -> SystemSettingsService.updateBorrowDays(guest, 10));
    }

    @Test
    void nonPositiveValuesAreRejected() {
        assertThrows(ValidationException.class,
                () -> SystemSettingsService.updateFineRate(owner, 0));
        assertThrows(ValidationException.class,
                () -> SystemSettingsService.updateMaxReservations(owner, -5));
    }

    @Test
    void updatesSurviveAResetReloadCycle() {
        SystemSettingsService.updateBorrowDays(owner, 5);
        SystemSettingsService.updateFineRate(owner, 20_000);
        SystemSettingsService.updateReservationDays(owner, 3);
        SystemSettingsService.updateMaxReservations(owner, 1);

        SystemSettingsService.reset();

        assertEquals(5, SystemSettingsService.getBorrowDays());
        assertEquals(20_000, SystemSettingsService.getFineRate());
        assertEquals(3, SystemSettingsService.getReservationDays());
        assertEquals(1, SystemSettingsService.getMaxReservations());
    }

    @Test
    void maxReservationsActsAsACeilingOverTheRolesNaturalLimit() {
        Persona teacher = PersonaService.registerPersona("teacher@test.com", "pass");
        teacher.updateRole(UserRole.TEACHER);
        PersonaRepository.insertPersona(teacher);
        ReservationService.reset();

        SystemSettingsService.updateMaxReservations(owner, 1);

        LibraryService.getAllItems().stream()
                .filter(item -> item.canReserve())
                .limit(2)
                .forEach(item -> ReservationService.reserve(teacher.getMemberId(), item.getItemId(),
                        SimulationClock.getCurrentDay()));

        assertTrue(ReservationService.getActiveReservationCount(teacher.getMemberId()) <= 1);
    }

    @Test
    void borrowDaysActsAsACeilingOverAnItemsNaturalBorrowPeriod() {
        SystemSettingsService.updateBorrowDays(owner, 3);

        LoanService.recordLoan("MEMBER-1", "ITEM-1", 0, Math.min(21, SystemSettingsService.getBorrowDays()));

        assertEquals(3, LoanService.getDueDay("MEMBER-1", "ITEM-1"));
    }
}
