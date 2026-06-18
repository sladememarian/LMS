package ir.ac.kntu.persona;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaServiceTest {

    private String unique() {
        return "persona_" + System.nanoTime() + "@test.com";
    }

    @Test
    void defaultStaffAccountsExist() {
        assertNotNull(PersonaService.getProfileByUsername("admin"));
        assertNotNull(PersonaService.getProfileByUsername("callcenter"));
        assertEquals(UserRole.ADMIN, PersonaService.getProfileByUsername("admin").getRole());
        assertEquals(UserRole.CALLCENTER, PersonaService.getProfileByUsername("callcenter").getRole());
    }

    @Test
    void registerAndValidate() {
        String email = unique();
        Persona persona = PersonaService.registerPersona(email, "Passw0rd!");
        assertEquals(UserRole.GUEST, persona.getRole());
        assertTrue(PersonaService.validateCredentials(email, "Passw0rd!"));
        assertFalse(PersonaService.validateCredentials(email, "wrong"));
    }

    @Test
    void updateProfileAndTheme() {
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        PersonaService.updateProfile(email, "Ali", "Reza", "09120000000");
        Persona persona = PersonaService.getProfile(email);
        assertEquals("Ali", persona.getFirstName());
        assertEquals("Reza", persona.getLastName());
        assertEquals("09120000000", persona.getPhoneNumber());
        PersonaService.updateTheme(email, "DARK");
        assertEquals("DARK", PersonaService.getProfile(email).getTheme());
    }

    @Test
    void updatePasswordChangesCredentials() {
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        assertTrue(PersonaService.updatePassword(email, "NewPass1!"));
        assertTrue(PersonaService.validateCredentials(email, "NewPass1!"));
        assertFalse(PersonaService.updatePassword("ghost@none.com", "X"));
    }

    @Test
    void walletOperations() {
        String email = unique();
        PersonaService.registerPersona(email, "Passw0rd!");
        assertEquals(0, PersonaService.getWalletBalance(email));
        PersonaService.updateWalletBalance(email, 500);
        assertEquals(500, PersonaService.getWalletBalance(email));
    }

    @Test
    void getProfileUnknownReturnsNull() {
        assertNull(PersonaService.getProfile("nobody_" + System.nanoTime() + "@x.com"));
    }
}
