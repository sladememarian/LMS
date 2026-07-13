package ir.ac.kntu.finance;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceDebtTest {
    // Debt: the reason libraries have that "please pay" look

    private Persona freshUser() {
        String email = "debt_" + System.nanoTime() + "@test.com";
        PersonaService.registerPersona(email, "Passw0rd!");
        return PersonaService.getProfile(email);
    }

    @Test
    void recordDebtIncreasesOutstandingAndBlocksBorrowing() {
        // Owe money? No books for you!
        Persona user = freshUser();
        assertEquals(0, FinanceService.getOutstandingDebt(user.getMemberId()));
        FinanceService.recordDebt(user, 30_000, "overdue item");
        assertEquals(30_000, FinanceService.getOutstandingDebt(user.getMemberId()));
        assertFalse(FinanceService.checkBorrowingPermission(user.getMemberId()));
    }

    @Test
    void recordDebtRejectsNonPositive() {
        // Zero debt? In this economy?
        Persona user = freshUser();
        assertThrows(IllegalArgumentException.class, () -> FinanceService.recordDebt(user, 0, "x"));
    }

    @Test
    void payDebtClearsOutstandingAndRestoresBorrowing() {
        // Money received. You may now borrow again.
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        FinanceService.recordDebt(user, 20_000, "overdue");
        FinanceService.payDebt(user);
        assertEquals(0, FinanceService.getOutstandingDebt(user.getMemberId()));
        assertTrue(FinanceService.checkBorrowingPermission(user.getMemberId()));
    }

    @Test
    void payDebtFailsWithoutDebt() {
        // Can't pay what you don't owe (philosophical)
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        assertThrows(ir.ac.kntu.exception.ValidationException.class,
                () -> FinanceService.payDebt(user));
    }

    @Test
    void transactionHistoryRecordsCharges() {
        // Transaction history: the receipt you didn't ask for
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 5_000);
        FinanceService.proccessWalletCharge(user, 7_000);
        assertEquals(2, FinanceService.getTransactionsForMember(user.getMemberId()).size());
    }

    @Test
    void taxRevenueAccumulates() {
        // Taxes: the only certainties in life and simulation
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        int before = FinanceService.getTaxRevenueCollected();
        FinanceService.proccessExtentionPayment(user, 10_000);
        assertTrue(FinanceService.getTaxRevenueCollected() > before);
    }
}