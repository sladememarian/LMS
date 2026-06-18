package ir.ac.kntu.finance;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the debt accounting, transaction history accessors and tax-revenue
 * aggregation added to FinanceService.
 */
class FinanceDebtTest {

    private Persona freshUser() {
        String email = "debt_" + System.nanoTime() + "@test.com";
        PersonaService.registerPersona(email, "Passw0rd!");
        return PersonaService.getProfile(email);
    }

    @Test
    void recordDebtIncreasesOutstandingAndBlocksBorrowing() {
        Persona user = freshUser();
        assertEquals(0, FinanceService.getOutstandingDebt(user.getMemberId()));
        FinanceService.recordDebt(user, 30_000, "overdue item");
        assertEquals(30_000, FinanceService.getOutstandingDebt(user.getMemberId()));
        assertFalse(FinanceService.checkBorrowingPermission(user.getMemberId()));
    }

    @Test
    void recordDebtRejectsNonPositive() {
        Persona user = freshUser();
        assertThrows(IllegalArgumentException.class, () -> FinanceService.recordDebt(user, 0, "x"));
    }

    @Test
    void payDebtClearsOutstandingAndRestoresBorrowing() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        FinanceService.recordDebt(user, 20_000, "overdue");
        assertTrue(FinanceService.payDebt(user));
        assertEquals(0, FinanceService.getOutstandingDebt(user.getMemberId()));
        assertTrue(FinanceService.checkBorrowingPermission(user.getMemberId()));
    }

    @Test
    void payDebtFailsWithoutDebt() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        assertFalse(FinanceService.payDebt(user));
    }

    @Test
    void transactionHistoryRecordsCharges() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 5_000);
        FinanceService.proccessWalletCharge(user, 7_000);
        assertEquals(2, FinanceService.getTransactionsForMember(user.getMemberId()).size());
    }

    @Test
    void taxRevenueAccumulates() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        int before = FinanceService.getTaxRevenueCollected();
        FinanceService.proccessExtentionPayment(user, 10_000);
        assertTrue(FinanceService.getTaxRevenueCollected() > before);
    }
}
