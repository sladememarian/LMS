package ir.ac.kntu.persona;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the per-user inventory tracking added to Persona and the
 * borrow/return/role-promotion persistence helpers in PersonaService.
 */
class PersonaInventoryTest {

    private String unique() {
        return "inv_" + System.nanoTime() + "@test.com";
    }

    @Test
    void personaTracksBorrowedItems() {
        Persona persona = new Persona("e@test.com", "Passw0rd!");
        assertEquals(0, persona.getBorrowCount());
        persona.addBorrowedItem("ITEM-001");
        persona.addBorrowedItem("ITEM-002");
        assertEquals(2, persona.getBorrowCount());
        assertTrue(persona.hasBorrowed("ITEM-001"));
        assertTrue(persona.removeBorrowedItem("ITEM-001"));
        assertFalse(persona.hasBorrowed("ITEM-001"));
        assertEquals(1, persona.getBorrowCount());
    }

    @Test
    void blankBorrowIdsAreIgnored() {
        Persona persona = new Persona("e2@test.com", "Passw0rd!");
        persona.addBorrowedItem("");
        persona.addBorrowedItem(null);
        assertEquals(0, persona.getBorrowCount());
    }

    @Test
    void recordBorrowAndReturnPersist() {
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        PersonaService.recordBorrow(email, "ITEM-003");
        assertTrue(PersonaService.getProfile(email).hasBorrowed("ITEM-003"));
        assertTrue(PersonaService.recordReturn(email, "ITEM-003"));
        assertFalse(PersonaService.getProfile(email).hasBorrowed("ITEM-003"));
        assertFalse(PersonaService.recordReturn(email, "ITEM-999"));
    }

    @Test
    void promoteRoleChangesRoleAndPrefix() {
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        assertTrue(PersonaService.promoteRole(email, UserRole.STUDENT));
        Persona persona = PersonaService.getProfile(email);
        assertEquals(UserRole.STUDENT, persona.getRole());
        assertTrue(persona.getMemberId().startsWith("STU-"));
        assertFalse(PersonaService.promoteRole("ghost@none.com", UserRole.TEACHER));
    }
}
