package ir.ac.kntu.iam;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ir.ac.kntu.exception.DuplicateEmailException;
import ir.ac.kntu.exception.InvalidEmailFormatException;
import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class IamServiceTest {

    // IAM: the digital velvet rope

    private String unique() {
        return "iam_" + System.nanoTime() + "@test.com";
    }

    @Test
    void registerUserCreatesProfileAndWelcomeMail() {
        // A star is born (and welcomed via email)
        String email = unique();
        IamService.registerUser(
            new UserCredentials(
                email,
                "Passw0rd!",
                "Sara",
                "Karimi",
                "09121112233"
            )
        );
        Persona persona = PersonaService.getProfile(email);
        assertEquals(UserRole.GUEST, persona.getRole());
        assertEquals("Sara", persona.getFirstName());
        Inbox inbox = MailService.getInbox(email);
        assertEquals(
            MessageType.WELCOME,
            inbox.getMessages().get(0).getMessageType()
        );
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrent() {
        // Old password, new password, same existential crisis
        String email = unique();
        IamService.registerUser(
            new UserCredentials(email, "Passw0rd!", "A", "B", "09121112233")
        );
        assertDoesNotThrow(() -> IamService.changePassword(email, "Passw0rd!", "Brandnew1!"));
        assertTrue(PersonaService.validateCredentials(email, "Brandnew1!"));
    }

    @Test
    void invalidRegistrationThrows() {
        // "bad-email" is indeed a bad email, shocker
        assertThrows(InvalidEmailFormatException.class, () ->
            IamService.registerUser(
                new UserCredentials(
                    "bad-email",
                    "Passw0rd!",
                    "A",
                    "B",
                    "09121112233"
                )
            )
        );
    }

    @Test
    void duplicateEmailRegistrationThrows() {
        // Signing up twice with the same email should not silently overwrite the account
        String email = unique();
        IamService.registerUser(
            new UserCredentials(
                email,
                "Passw0rd!",
                "Sara",
                "Karimi",
                "09121112233"
            )
        );
        assertThrows(DuplicateEmailException.class, () ->
            IamService.registerUser(
                new UserCredentials(
                    email,
                    "Different1!",
                    "Other",
                    "Name",
                    "09121112233"
                )
            )
        );
    }

    @Test
    void loginMenuPrintsFriendlyMessageAndKeepsRunningAfterWrongPassword() {
        // The console should catch InvalidCredentialsException, print a friendly
        // message, and let the user try again instead of crashing.
        String email = unique();
        IamService.registerUser(
            new UserCredentials(email, "Passw0rd!", "A", "B", "09121112233")
        );

        String simulatedInput = email + "\nWrongPassword1!\n0\n";
        Scanner scanner = new Scanner(
            new ByteArrayInputStream(
                simulatedInput.getBytes(StandardCharsets.UTF_8)
            )
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            assertDoesNotThrow(() -> IamService.loginMenu(scanner));
        } finally {
            System.setOut(original);
        }

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(
            printed.contains("Incorrect email or password"),
            "Expected the friendly InvalidCredentialsException message to be printed"
        );
        assertFalse(
            printed.contains("Login successful"),
            "Login should not have succeeded with the wrong password"
        );
    }
}
