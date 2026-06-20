package ir.ac.kntu.persona;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaServiceTest {
    // Persona service: the person factory

    private String unique() {
        return "persona_" + System.nanoTime() + "@test.com";
    }

    @Test
    void registerCreatesGuestAndValidates() {
        // Guest: the "free trial" of user roles
        String email = unique();
        Persona persona = PersonaService.registerPersona(email, "Passw0rd!");
        assertEquals(UserRole.GUEST, persona.getRole());
        assertTrue(PersonaService.validateCredentials(email, "Passw0rd!"));
        assertFalse(PersonaService.validateCredentials(email, "wrong"));
    }

    @Test
    void promoteRoleUpgradesPersona() {
        // Level up! You are now a STUDENT. +5 to book borrowing.
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        assertTrue(PersonaService.promoteRole(email, UserRole.STUDENT));
        assertEquals(UserRole.STUDENT, PersonaService.getProfile(email).getRole());
    }

    @Test
    void walletOperations() {
        // Wallet: where money goes to die (and occasionally grow)
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        assertEquals(0, PersonaService.getWalletBalance(email));
        PersonaService.updateWalletBalance(email, 500);
        assertEquals(500, PersonaService.getWalletBalance(email));
    }

    @Test
    void lookupByMemberId() {
        // Find by ID: the database's favorite party trick
        String email = unique();
        Persona persona = PersonaService.registerPersona(email, "Passw0rd!");
        Persona found = PersonaService.getProfileByMemberId(persona.getMemberId());
        assertNotNull(found);
        assertEquals(email, found.getEmail());
        assertNull(PersonaService.getProfileByMemberId("NOPE-000000"));
    }
}