package ir.ac.kntu.finance;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanSimulationTest {
    // Loan simulation: where days are fake but debt is real

    private static final int DAILY_FINE = 10_000;

    private Persona freshUser() {
        String email = "loan_" + System.nanoTime() + "@test.com";
        return PersonaService.registerPersona(email, "Passw0rd!");
    }

    @Test
    void clockAdvancesAndPersists() {
        // Day + 1. Groundhog day with more math.
        int before = SimulationClock.getCurrentDay();
        int after = SimulationClock.advanceDay();
        assertEquals(before + 1, after);
        assertEquals(after, SimulationClock.getCurrentDay());
    }

    @Test
    void overdueLoanAccruesDebtExactlyOncePerDay() {
        // Late fees: the gift that keeps on giving (once per day)
        Persona user = freshUser();
        String memberId = user.getMemberId();
        int day = SimulationClock.getCurrentDay();
        LoanService.recordLoan(memberId, "ITEM-001", day);

        LoanService.accrueOverdueDebts(day + 3);
        assertEquals(0, FinanceService.getOutstandingDebt(memberId));

        LoanService.accrueOverdueDebts(day + 4);
        assertEquals(DAILY_FINE, FinanceService.getOutstandingDebt(memberId));

        LoanService.accrueOverdueDebts(day + 4);
        assertEquals(DAILY_FINE, FinanceService.getOutstandingDebt(memberId));
        assertFalse(FinanceService.checkBorrowingPermission(memberId));
    }

    @Test
    void extendLoanPushesDueDayAndAvoidsFine() {
        // Extending: like a pause button for overdue fines
        Persona user = freshUser();
        String memberId = user.getMemberId();
        int day = SimulationClock.getCurrentDay();
        LoanService.recordLoan(memberId, "ITEM-EXT", day);

        assertEquals(day + 3, LoanService.getDueDay(memberId, "ITEM-EXT"));

        assertTrue(LoanService.extendLoan(memberId, "ITEM-EXT", 3));
        assertEquals(day + 6, LoanService.getDueDay(memberId, "ITEM-EXT"));

        LoanService.accrueOverdueDebts(day + 6);
        assertEquals(0, FinanceService.getOutstandingDebt(memberId));

        LoanService.accrueOverdueDebts(day + 7);
        assertEquals(DAILY_FINE, FinanceService.getOutstandingDebt(memberId));
    }

    @Test
    void returnedLoanDoesNotAccrueDebt() {
        // Returned? No debt. It's that simple, Karen.
        Persona user = freshUser();
        String memberId = user.getMemberId();
        int day = SimulationClock.getCurrentDay();
        LoanService.recordLoan(memberId, "ITEM-002", day);
        assertTrue(LoanService.clearLoan(memberId, "ITEM-002"));
        LoanService.accrueOverdueDebts(day + 5);
        assertEquals(0, FinanceService.getOutstandingDebt(memberId));
    }
}