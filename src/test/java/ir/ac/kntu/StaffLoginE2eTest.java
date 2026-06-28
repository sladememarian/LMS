package ir.ac.kntu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportConsole;
import ir.ac.kntu.util.Database;
import ir.ac.kntu.util.DatabaseAccess;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the staff (admin / callcenter) email-based login flow.
 *
 * Each test seeds fresh staff accounts directly into the DB, then exercises
 * the service layer the same way the app does when a user picks option 2 (Login)
 * and option 5 (Support) from the main menu.
 */
class StaffLoginE2eTest {

    @BeforeAll
    static void connectDb() {
        Database.getConnection();
    }

    @BeforeEach
    void seedStaff() {
        DatabaseAccess.clearPersonas();

        Persona admin = new Persona("admin@system.local", "adminpass");
        admin.updateRole(UserRole.ADMIN);
        DatabaseAccess.insertPersona(admin);

        Persona cc = new Persona("callcenter@system.local", "ccpass");
        cc.updateRole(UserRole.CALLCENTER);
        DatabaseAccess.insertPersona(cc);

        Persona.setCurrentUser(null);
    }

    // ── credential validation ────────────────────────────────────────────────

    @Test
    void adminEmailPasswordIsAccepted() {
        assertTrue(PersonaService.validateCredentials("admin@system.local", "adminpass"));
    }

    @Test
    void adminWrongPasswordIsRejected() {
        assertFalse(PersonaService.validateCredentials("admin@system.local", "wrongpass"));
    }

    @Test
    void adminOldUsernameFormatIsRejected() {
        // username-only login was removed; "admin" without domain must not work
        assertFalse(PersonaService.validateCredentials("admin", "adminpass"));
    }

    @Test
    void callcenterEmailPasswordIsAccepted() {
        assertTrue(PersonaService.validateCredentials("callcenter@system.local", "ccpass"));
    }

    @Test
    void callcenterWrongPasswordIsRejected() {
        assertFalse(PersonaService.validateCredentials("callcenter@system.local", "wrongpass"));
    }

    // ── profile / role correctness ───────────────────────────────────────────

    @Test
    void adminProfileHasAdminRoleAndCorrectEmail() {
        // validateCredentials reloads the in-memory list from DB
        PersonaService.validateCredentials("admin@system.local", "adminpass");
        Persona admin = PersonaService.getProfile("admin@system.local");

        assertNotNull(admin);
        assertEquals("admin@system.local", admin.getEmail());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertNotNull(admin.getMemberId());
        assertTrue(admin.getMemberId().startsWith("ADM-"));
    }

    @Test
    void callcenterProfileHasCallcenterRoleAndCorrectEmail() {
        PersonaService.validateCredentials("callcenter@system.local", "ccpass");
        Persona cc = PersonaService.getProfile("callcenter@system.local");

        assertNotNull(cc);
        assertEquals("callcenter@system.local", cc.getEmail());
        assertEquals(UserRole.CALLCENTER, cc.getRole());
        assertNotNull(cc.getMemberId());
        assertTrue(cc.getMemberId().startsWith("CC-"));
    }

    // ── support console routing ──────────────────────────────────────────────

    @Test
    void supportConsoleRequiresLogin() {
        Persona.setCurrentUser(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(out));
        try {
            SupportConsole.open(new Scanner(new ByteArrayInputStream("".getBytes())));
        } finally {
            System.setOut(old);
        }
        assertTrue(out.toString().contains("Log in first"),
                "Expected 'Log in first' message for unauthenticated access");
    }

    @Test
    void supportConsoleRoutesAdminToAdminInbox() {
        PersonaService.validateCredentials("admin@system.local", "adminpass");
        Persona admin = PersonaService.getProfile("admin@system.local");
        Persona.setCurrentUser(admin);

        // Feed "0" to immediately exit the AdminInbox menu
        Scanner scanner = new Scanner(new ByteArrayInputStream("0\n".getBytes()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(out));
        try {
            assertDoesNotThrow(() -> SupportConsole.open(scanner));
        } finally {
            System.setOut(old);
        }
        assertTrue(out.toString().contains("ADMIN INBOX"),
                "Expected AdminInbox banner for ADMIN role");
    }

    @Test
    void supportConsoleRoutesCallcenterToCallcenterInbox() {
        PersonaService.validateCredentials("callcenter@system.local", "ccpass");
        Persona cc = PersonaService.getProfile("callcenter@system.local");
        Persona.setCurrentUser(cc);

        Scanner scanner = new Scanner(new ByteArrayInputStream("0\n".getBytes()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(out));
        try {
            assertDoesNotThrow(() -> SupportConsole.open(scanner));
        } finally {
            System.setOut(old);
        }
        assertTrue(out.toString().contains("CALLCENTER INBOX"),
                "Expected CallcenterInbox banner for CALLCENTER role");
    }

    @Test
    void supportConsoleMemberSeesUserInbox() {
        Persona member = new Persona("member@test.com", "pass123");
        DatabaseAccess.insertPersona(member);
        PersonaService.validateCredentials("member@test.com", "pass123");
        Persona loaded = PersonaService.getProfile("member@test.com");
        Persona.setCurrentUser(loaded);

        // Feed "0" to immediately exit the support member console
        Scanner scanner = new Scanner(new ByteArrayInputStream("0\n".getBytes()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(out));
        try {
            assertDoesNotThrow(() -> SupportConsole.open(scanner));
        } finally {
            System.setOut(old);
        }
        // Member should NOT see admin or callcenter inbox
        assertFalse(out.toString().contains("ADMIN INBOX"));
        assertFalse(out.toString().contains("CALLCENTER INBOX"));
    }

    // ── regular user regression ──────────────────────────────────────────────

    @Test
    void regularUserCanStillLoginByEmail() {
        Persona user = new Persona("student@test.com", "pass123");
        DatabaseAccess.insertPersona(user);

        assertTrue(PersonaService.validateCredentials("student@test.com", "pass123"));

        PersonaService.validateCredentials("student@test.com", "pass123");
        Persona loaded = PersonaService.getProfile("student@test.com");
        assertNotNull(loaded);
        assertEquals(UserRole.GUEST, loaded.getRole());
        assertEquals("student@test.com", loaded.getEmail());
    }

    @Test
    void regularUserCredentialsDoNotGrantStaffAccess() {
        Persona user = new Persona("hacker@test.com", "adminpass");
        DatabaseAccess.insertPersona(user);

        PersonaService.validateCredentials("hacker@test.com", "adminpass");
        Persona loaded = PersonaService.getProfile("hacker@test.com");
        assertNotNull(loaded);
        assertNotEquals(UserRole.ADMIN, loaded.getRole());
        assertNotEquals(UserRole.CALLCENTER, loaded.getRole());
    }

    @Test
    void adminCredentialsDoNotMatchRegularUser() {
        // A regular user registered with admin@system.local email should not exist
        // (admin is already seeded in @BeforeEach, re-registration with same email
        //  would conflict, but validateCredentials won't grant wrong role)
        PersonaService.validateCredentials("admin@system.local", "adminpass");
        Persona admin = PersonaService.getProfile("admin@system.local");
        assertNotEquals(UserRole.GUEST, admin.getRole());
    }
}
