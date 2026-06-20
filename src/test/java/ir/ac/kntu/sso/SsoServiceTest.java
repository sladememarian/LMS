package ir.ac.kntu.sso;

import ir.ac.kntu.iam.IamService;
import ir.ac.kntu.iam.UserCredentials;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.sso.SsoService;
import ir.ac.kntu.sso.SessionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsoServiceTest {
    // SSO: one ring to log them all

    private String register() {
        String email = "sso_" + System.nanoTime() + "@test.com";
        IamService.registerUser(new UserCredentials(email, "Passw0rd!", "First", "Last", "09120000000"));
        return email;
    }

    @Test
    void viewProfileContainsIdentity() {
        // Profile: contains everything but your soul
        String email = register();
        String profile = SsoService.viewProfile(email);
        assertTrue(profile.contains("First"));
        assertTrue(profile.contains("Last"));
        assertTrue(profile.contains(email));
    }

    @Test
    void editProfileUpdatesFields() {
        // Neo Anderson: the chosen one of unit tests
        String email = register();
        SsoService.editProfile(email, "Neo", "Anderson", "09129998877");
        Persona persona = PersonaService.getProfile(email);
        assertEquals("Neo", persona.getFirstName());
        assertEquals("09129998877", persona.getPhoneNumber());
    }

    @Test
    void editProfileRejectsInvalidPhone() {
        // "not-a-phone" - well actually it's a string
        String email = register();
        assertThrows(IllegalArgumentException.class,
                () -> SsoService.editProfile(email, "X", "Y", "not-a-phone"));
    }

    @Test
    void changePasswordRequiresMatchingConfirm() {
        // Passwords must match. Computers are literal.
        String email = register();
        assertThrows(IllegalArgumentException.class,
                () -> SsoService.changePassword(email, "Passw0rd!", "Newpass1!", "Mismatch1!"));
        assertTrue(SsoService.changePassword(email, "Passw0rd!", "Newpass1!", "Newpass1!"));
    }

    @Test
    void themeSettingsValidatedAndPersisted() {
        // #000000 is the new black
        String email = register();
        SsoService.changeTheme(email, "dark");
        assertEquals("DARK", SsoService.getTheme(email));
        assertThrows(IllegalArgumentException.class, () -> SsoService.changeTheme(email, "NEON"));
    }

    @Test
    void sessionLifecycleAndLogout() {
        // You can check out any time you like, but you can never leave... jk you just log out
        String email = register();
        Persona persona = PersonaService.getProfile(email);
        SessionManager.createSession(persona);
        assertTrue(SessionManager.isActive());
        assertNotNull(SessionManager.getSessionToken());
        SsoService.logout();
        assertFalse(SessionManager.isActive());
    }

    @Test
    void createSessionRejectsNull() {
        // Throws on null: protecting the world from NullPointerException one session at a time
        assertThrows(IllegalArgumentException.class, () -> SessionManager.createSession(null));
    }
}