package ir.ac.kntu;

import ir.ac.kntu.finance.Loan;
import ir.ac.kntu.finance.Transaction;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportTicket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Core domain-model behaviour, including the fields added for the new features:
 * a Transaction timestamp (history sorting), a SupportTicket response (call
 * centre replies) and the Loan due-day model (date-simulation debts).
 */
class DomainModelsTest {

    @Test
    void transactionCarriesTimestamp() {
        Transaction auto = new Transaction("TX-1", "STU-1", 250, "CHARGE", "topup");
        assertEquals(250, auto.getAmount());
        assertTrue(auto.getTimestamp() > 0, "default constructor stamps creation time");
        Transaction restored = new Transaction("TX-2", "STU-1", 10, "DEBT", "d", 12_345L);
        assertEquals(12_345L, restored.getTimestamp());
    }

    @Test
    void supportTicketStoresResponse() {
        SupportTicket ticket = new SupportTicket("T1", "U1", "title", "desc");
        assertEquals("", ticket.getResponse());
        ticket.setResponse("We are on it");
        assertEquals("We are on it", ticket.getResponse());
    }

    @Test
    void loanTracksDueAndChargedDays() {
        Loan loan = new Loan("STU-1", "ITEM-001", 3, 4);
        assertEquals("ITEM-001", loan.getItemId());
        assertEquals(3, loan.getBorrowDay());
        assertEquals(4, loan.getDueDay());
        assertEquals(4, loan.getLastChargedDay());
        loan.setLastChargedDay(5);
        assertEquals(5, loan.getLastChargedDay());
    }

    @Test
    void personaRoleUpgradeRegeneratesMemberId() {
        Persona guest = new Persona("g@test.com", "Passw0rd!");
        assertEquals(UserRole.GUEST, guest.getRole());
        assertTrue(guest.getMemberId().startsWith("GST-"));
        guest.updateRole(UserRole.STUDENT);
        assertEquals(UserRole.STUDENT, guest.getRole());
        assertTrue(guest.getMemberId().startsWith("STU-"));
    }

    @Test
    void userRoleBorrowLimits() {
        assertEquals(2, UserRole.GUEST.getMaxBorrowLimit());
        assertEquals(10, UserRole.STUDENT.getMaxBorrowLimit());
        assertEquals(15, UserRole.TEACHER.getMaxBorrowLimit());
    }
}
