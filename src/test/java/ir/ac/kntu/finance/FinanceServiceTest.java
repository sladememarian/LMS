package ir.ac.kntu.finance;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceServiceTest {

    private Persona freshUser() {
        String email = "fin_" + System.nanoTime() + "@test.com";
        PersonaService.registerPersona(email, "Passw0rd!");
        return PersonaService.getProfile(email);
    }

    @Test
    void walletChargeAddsFunds() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 1_000);
        assertEquals(1_000, PersonaService.getWalletBalance(user.getEmail()));
    }

    @Test
    void walletChargeRejectsNonPositive() {
        Persona user = freshUser();
        assertThrows(IllegalArgumentException.class, () -> FinanceService.proccessWalletCharge(user, 0));
    }

    @Test
    void extensionPaymentSucceedsWithTax() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 1_000);
        assertTrue(FinanceService.proccessExtentionPayment(user, 100));
        assertEquals(890, PersonaService.getWalletBalance(user.getEmail()));
    }

    @Test
    void extensionPaymentFailsWhenInsufficient() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 50);
        assertFalse(FinanceService.proccessExtentionPayment(user, 100));
    }

    @Test
    void extensionPaymentRejectsNonPositive() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 500);
        assertFalse(FinanceService.proccessExtentionPayment(user, 0));
    }

    @Test
    void debtBlocksBorrowingPermission() {
        Persona user = freshUser();
        FinanceService.proccessWalletCharge(user, 1_000);
        assertTrue(FinanceService.checkBorrowingPermission(user.getMemberId()));
        FinanceService.proccessExtentionPayment(user, 100);
        assertFalse(FinanceService.checkBorrowingPermission(user.getMemberId()));
    }
}
