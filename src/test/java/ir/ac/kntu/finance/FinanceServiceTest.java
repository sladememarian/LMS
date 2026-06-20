package ir.ac.kntu.finance;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finance wallet, debt and tax behaviour plus the time-ordered transaction
 * history (feature: history sorted by time).
 */
class FinanceServiceTest {

    private Persona freshUser() {
        String email = "fin_" + System.nanoTime() + "@test.com";
        return PersonaService.registerPersona(email, "Passw0rd!");
    }

    @Test
    void chargeRecordsTransactionHistory() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 5_000);
        FinanceService.proccessWalletCharge(user, 7_000);
        assertEquals(2, FinanceService.getTransactionsForMember(user.getMemberId()).size());
    }

    @Test
    void debtBlocksBorrowingAndPaymentClearsIt() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        FinanceService.recordDebt(user, 20_000, "overdue item");
        assertEquals(20_000, FinanceService.getOutstandingDebt(user.getMemberId()));
        assertFalse(FinanceService.checkBorrowingPermission(user.getMemberId()));
        assertTrue(FinanceService.payDebt(user));
        assertEquals(0, FinanceService.getOutstandingDebt(user.getMemberId()));
        assertTrue(FinanceService.checkBorrowingPermission(user.getMemberId()));
    }

    @Test
    void taxRevenueAccumulatesOnExtension() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 100_000);
        int before = FinanceService.getTaxRevenueCollected();
        assertTrue(FinanceService.proccessExtentionPayment(user, 10_000));
        assertTrue(FinanceService.getTaxRevenueCollected() > before);
    }

    @Test
    void transactionHistoryIsSortedByTime() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 1_000);
        FinanceService.proccessWalletCharge(user, 2_000);
        FinanceService.proccessWalletCharge(user, 3_000);
        List<Transaction> history = FinanceService.getTransactionsForMember(user.getMemberId());
        assertTrue(history.size() >= 3);
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i - 1).getTimestamp() <= history.get(i).getTimestamp(),
                    "history must be ordered oldest -> newest");
        }
    }
}
