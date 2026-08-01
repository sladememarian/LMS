package ir.ac.kntu.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ir.ac.kntu.gui.signup.SignupEnvelope;

import org.junit.jupiter.api.Test;

/**
 * Round-trips a {@link SignupEnvelope} through its JSON-line form. Runs in CI
 * (package {@code ir.ac.kntu.util}, not {@code ir.ac.kntu.gui}) and needs no
 * JavaFX, so it validates the durable spool format without a display.
 */
class SignupEnvelopeTest {

    @Test
    void serializesAndParsesBackToTheSameFields() {
        SignupEnvelope original = new SignupEnvelope(
                "ada@system.local", "Ada", "Lovelace", "09120000000");
        String line = original.toJsonLine();

        SignupEnvelope parsed = SignupEnvelope.fromJsonLine(line);
        assertEquals("ada@system.local", parsed.getEmail());
        assertEquals("Ada", parsed.getFirstName());
        assertEquals("Lovelace", parsed.getLastName());
        assertEquals("09120000000", parsed.getPhoneNumber());
    }

    @Test
    void escapesQuotesAndBackslashesInNames() {
        SignupEnvelope original = new SignupEnvelope(
                "q@system.local", "A\"B", "C\\D", "09120000000");
        SignupEnvelope parsed = SignupEnvelope.fromJsonLine(original.toJsonLine());
        assertEquals("A\"B", parsed.getFirstName());
        assertEquals("C\\D", parsed.getLastName());
    }
}
