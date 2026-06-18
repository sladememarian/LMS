package ir.ac.kntu;

import ir.ac.kntu.finance.Transaction;
import ir.ac.kntu.iam.UserCredentials;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportTicket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelsTest {

    @Test
    void transactionGetters() {
        Transaction tx = new Transaction("TX-1", "STU-1", 250, "CHARGE", "topup");
        assertEquals("TX-1", tx.getTransactionId());
        assertEquals("STU-1", tx.getMemberId());
        assertEquals(250, tx.getAmount());
        assertEquals("CHARGE", tx.getType());
        assertEquals("topup", tx.getDescription());
    }

    @Test
    void supportTicketDefaultsAndOrdering() {
        SupportTicket low = new SupportTicket("T1", "U1", "low", "d");
        SupportTicket critical = new SupportTicket("T2", "U2", "crit", "d");
        critical.setPriority("CRITICAL");
        assertEquals("LOW", low.getPriority());
        assertTrue(critical.compareTo(low) < 0);
    }

    @Test
    void userCredentialsValidationAndSetters() {
        UserCredentials user = new UserCredentials("u@test.com", "Passw0rd!", "F", "L", "09120000000");
        assertEquals("u@test.com", user.getEmail());
        user.setPhoneNumber("09121234567");
        assertEquals("09121234567", user.getPhoneNumber());
        assertThrows(IllegalArgumentException.class,
                () -> new UserCredentials("bad", "Passw0rd!", "F", "L", "09120000000"));
        assertThrows(IllegalArgumentException.class, () -> user.setPassword("weak"));
    }

    @Test
    void userCredentialsEquality() {
        UserCredentials one = new UserCredentials("u@test.com", "Passw0rd!", "F", "L", "09120000000");
        UserCredentials two = new UserCredentials("u@test.com", "Passw0rd!", "F", "L", "09120000000");
        UserCredentials three = new UserCredentials("other@test.com", "Passw0rd!", "F", "L", "09120000000");
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
        assertNotEquals(one, three);
    }

    @Test
    void userRoleMetadata() {
        assertEquals("STU-", UserRole.STUDENT.getPrefix());
        assertEquals(10, UserRole.STUDENT.getMaxBorrowLimit());
        assertEquals("ADM-", UserRole.ADMIN.getPrefix());
        assertEquals(2, UserRole.GUEST.getMaxBorrowLimit());
    }

    @Test
    void personaConstructorsAndRoleUpdate() {
        Persona guest = new Persona("g@test.com", "Passw0rd!");
        assertEquals(UserRole.GUEST, guest.getRole());
        assertTrue(guest.getMemberId().startsWith("GST-"));
        guest.updateRole(UserRole.STUDENT);
        assertEquals(UserRole.STUDENT, guest.getRole());
        assertTrue(guest.getMemberId().startsWith("STU-"));
        assertEquals("LIGHT", guest.getTheme());
    }
}
