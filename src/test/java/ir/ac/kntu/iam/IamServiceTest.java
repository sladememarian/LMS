package ir.ac.kntu.iam;

import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamServiceTest {

    private String unique() {
        return "iam_" + System.nanoTime() + "@test.com";
    }

    private void register(String email, String first, String last) {
        IamService.registerUser(new UserCredentials(email, "Passw0rd!", first, last, "09121112233"));
    }

    @Test
    void registerUserCreatesPersonaProfileAndWelcomeMail() {
        String email = unique();
        register(email, "Sara", "Karimi");
        Persona persona = PersonaService.getProfile(email);
        assertEquals(UserRole.GUEST, persona.getRole());
        assertEquals("Sara", persona.getFirstName());
        Inbox inbox = MailService.getInbox(email);
        assertEquals(1, inbox.getMessages().size());
        assertEquals(MessageType.WELCOME, inbox.getMessages().get(0).getMessageType());
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrent() {
        String email = unique();
        register(email, "A", "B");
        assertTrue(IamService.changePassword(email, "Passw0rd!", "Brandnew1!"));
        assertTrue(PersonaService.validateCredentials(email, "Brandnew1!"));
    }

    @Test
    void changePasswordFailsWithWrongCurrent() {
        String email = unique();
        register(email, "A", "B");
        assertThrows(IllegalArgumentException.class,
                () -> IamService.changePassword(email, "wrongCurrent", "Brandnew1!"));
    }

    @Test
    void changePasswordRejectsWeakNewPassword() {
        String email = unique();
        register(email, "A", "B");
        assertThrows(IllegalArgumentException.class,
                () -> IamService.changePassword(email, "Passw0rd!", "weak"));
    }

    @Test
    void invalidRegistrationThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> IamService.registerUser(new UserCredentials("bad-email", "Passw0rd!", "A", "B", "09121112233")));
    }

    @Test
    void twoFactorCodeIsDeliverable() {
        String email = unique();
        String code = MailService.deliver2FACode(email);
        assertFalse(MailService.verifyCode(email, "111111"));
        assertTrue(MailService.verifyCode(email, code));
    }
}
