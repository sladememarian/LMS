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

    private String register() {
        String email = "sso_" + System.nanoTime() + "@test.com";
        IamService.registerUser(new UserCredentials(email, "Passw0rd!", "First", "Last", "09120000000"));
        return email;
    }

    @Test
    void viewProfileContainsIdentity() {
        String email = register();
        String profile = SsoService.viewProfile(email);
        assertTrue(profile.contains("First"));
        assertTrue(profile.contains("Last"));
        assertTrue(profile.contains(email));
    }

    @Test
    void editProfileUpdatesFields() {
        String email = register();
        SsoService.editProfile(email, "Neo", "Anderson", "09129998877");
        Persona persona = PersonaService.getProfile(email);
        assertEquals("Neo", persona.getFirstName());
        assertEquals("09129998877", persona.getPhoneNumber());
    }

    @Test
    void editProfileRejectsInvalidPhone() {
        String email = register();
        assertThrows(IllegalArgumentException.class,
                () -> SsoService.editProfile(email, "X", "Y", "not-a-phone"));
    }

    @Test
    void changePasswordRequiresMatchingConfirm() {
        String email = register();
        assertThrows(IllegalArgumentException.class,
                () -> SsoService.changePassword(email, "Passw0rd!", "Newpass1!", "Mismatch1!"));
        assertTrue(SsoService.changePassword(email, "Passw0rd!", "Newpass1!", "Newpass1!"));
    }

    @Test
    void themeSettingsValidatedAndPersisted() {
        String email = register();
        SsoService.changeTheme(email, "dark");
        assertEquals("DARK", SsoService.getTheme(email));
        assertThrows(IllegalArgumentException.class, () -> SsoService.changeTheme(email, "NEON"));
    }

    @Test
    void sessionLifecycleAndLogout() {
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
        assertThrows(IllegalArgumentException.class, () -> SessionManager.createSession(null));
    }
}
