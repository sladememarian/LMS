package ir.ac.kntu.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;


class IamServiceTest {
    // IAM: the digital velvet rope

    private String unique() {
        return "iam_" + System.nanoTime() + "@test.com";
    }

    @Test
    void registerUserCreatesProfileAndWelcomeMail() {
        // A star is born (and welcomed via email)
        String email = unique();
        IamService.registerUser(new UserCredentials(email, "Passw0rd!", "Sara", "Karimi", "09121112233"));
        Persona persona = PersonaService.getProfile(email);
        assertEquals(UserRole.GUEST, persona.getRole());
        assertEquals("Sara", persona.getFirstName());
        Inbox inbox = MailService.getInbox(email);
        assertEquals(MessageType.WELCOME, inbox.getMessages().get(0).getMessageType());
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrent() {
        // Old password, new password, same existential crisis
        String email = unique();
        IamService.registerUser(new UserCredentials(email, "Passw0rd!", "A", "B", "09121112233"));
        assertTrue(IamService.changePassword(email, "Passw0rd!", "Brandnew1!"));
        assertTrue(PersonaService.validateCredentials(email, "Brandnew1!"));
    }

    @Test
    void invalidRegistrationThrows() {
        // "bad-email" is indeed a bad email, shocker
        assertThrows(IllegalArgumentException.class,
                () -> IamService.registerUser(new UserCredentials("bad-email", "Passw0rd!", "A", "B", "09121112233")));
    }
}