package ir.ac.kntu.persona;

import ir.ac.kntu.exception.BaseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaInventoryTest {
    // Persona inventory: I have many leather-bound books

    private String unique() {
        return "inv_" + System.nanoTime() + "@test.com";
    }

    @Test
    void personaTracksBorrowedItems() {
        // Item borrowed. Item returned. Item tracked. Item forgotten.
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
        // "" and null are the invisible borrowers
        Persona persona = new Persona("e2@test.com", "Passw0rd!");
        persona.addBorrowedItem("");
        persona.addBorrowedItem(null);
        assertEquals(0, persona.getBorrowCount());
    }

    @Test
    void recordBorrowAndReturnPersist() {
        // Borrow, return, borrow again. The circle of library life.
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        PersonaService.recordBorrow(email, "ITEM-003");
        assertTrue(PersonaService.getProfile(email).hasBorrowed("ITEM-003"));
        assertDoesNotThrow(() -> PersonaService.recordReturn(email, "ITEM-003"));
        assertFalse(PersonaService.getProfile(email).hasBorrowed("ITEM-003"));
        assertDoesNotThrow(() -> PersonaService.recordReturn(email, "ITEM-999"));
    }

    @Test
    void promoteRoleChangesRoleAndPrefix() {
        // Promotion: now with extra prefix!
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        assertDoesNotThrow(() -> PersonaService.promoteRole(email, UserRole.STUDENT));
        Persona persona = PersonaService.getProfile(email);
        assertEquals(UserRole.STUDENT, persona.getRole());
        assertTrue(persona.getMemberId().startsWith("STU-"));
        assertThrows(BaseException.class, () -> PersonaService.promoteRole("ghost@none.com", UserRole.TEACHER));
    }
}