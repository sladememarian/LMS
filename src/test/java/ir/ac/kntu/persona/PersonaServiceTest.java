package ir.ac.kntu.persona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ir.ac.kntu.exception.DuplicateEmailException;
import ir.ac.kntu.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;

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
        assertEquals(
            UserRole.STUDENT,
            PersonaService.getProfile(email).getRole()
        );
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
        Persona found = PersonaService.getProfileByMemberId(
            persona.getMemberId()
        );
        assertNotNull(found);
        assertEquals(email, found.getEmail());
        assertNull(PersonaService.getProfileByMemberId("NOPE-000000"));
    }

    @Test
    void registeringSameEmailTwiceThrowsDuplicateEmailException() {
        // Two accounts, one email: not on our watch
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        assertThrows(DuplicateEmailException.class, () ->
            PersonaService.registerPersona(email, "OtherPassw0rd!")
        );
    }

    @Test
    void walletBalanceForUnknownEmailThrowsUserNotFoundException() {
        // No profile, no wallet, no balance to report
        assertThrows(UserNotFoundException.class, () ->
            PersonaService.getWalletBalance(
                "ghost_" + System.nanoTime() + "@nowhere.com"
            )
        );
    }
}
