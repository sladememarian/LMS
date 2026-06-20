package ir.ac.kntu.finance;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Date-simulation feature: the Admin advances {@link SimulationClock} and
 * {@link LoanService} accrues overdue fines into member debts by reusing the
 * existing {@link FinanceService#recordDebt} API. A loan is due one simulated
 * day after it is taken; a fine is charged for each day the simulated date is
 * strictly past the due day, and never more than once per simulated day.
 */
class LoanSimulationTest {

    private static final int DAILY_FINE = 10_000;

    private Persona freshUser() {
        String email = "loan_" + System.nanoTime() + "@test.com";
        return PersonaService.registerPersona(email, "Passw0rd!");
    }

    @Test
    void clockAdvancesAndPersists() {
        int before = SimulationClock.getCurrentDay();
        int after = SimulationClock.advanceDay();
        assertEquals(before + 1, after);
        assertEquals(after, SimulationClock.getCurrentDay());
    }

    @Test
    void overdueLoanAccruesDebtExactlyOncePerDay() {
        Persona user = freshUser();
        String memberId = user.getMemberId();
        int day = SimulationClock.getCurrentDay();
        LoanService.recordLoan(memberId, "ITEM-001", day);

        // Due day is day+3, so day+3 is the deadline (not yet overdue).
        LoanService.accrueOverdueDebts(day + 3);
        assertEquals(0, FinanceService.getOutstandingDebt(memberId));

        // day+4 is one day past due -> one daily fine injected as debt.
        LoanService.accrueOverdueDebts(day + 4);
        assertEquals(DAILY_FINE, FinanceService.getOutstandingDebt(memberId));

        // Re-running the same simulated day must not double-charge.
        LoanService.accrueOverdueDebts(day + 4);
        assertEquals(DAILY_FINE, FinanceService.getOutstandingDebt(memberId));
        assertFalse(FinanceService.checkBorrowingPermission(memberId));
    }

    @Test
    void returnedLoanDoesNotAccrueDebt() {
        Persona user = freshUser();
        String memberId = user.getMemberId();
        int day = SimulationClock.getCurrentDay();
        LoanService.recordLoan(memberId, "ITEM-002", day);
        assertTrue(LoanService.clearLoan(memberId, "ITEM-002"));
        LoanService.accrueOverdueDebts(day + 5);
        assertEquals(0, FinanceService.getOutstandingDebt(memberId));
    }
}
