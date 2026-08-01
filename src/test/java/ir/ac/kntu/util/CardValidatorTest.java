package ir.ac.kntu.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ir.ac.kntu.gui.util.CardValidator;

/**
 * Plain JUnit (no JavaFX) coverage of the GUI-side card validation. Lives under
 * {@code ir.ac.kntu.util} (not {@code ir.ac.kntu.gui}) so it still runs in CI,
 * where the TestFX GUI suite is excluded via {@code -PskipGuiTests}.
 */
class CardValidatorTest {

    @Test
    void accepts16DigitCardWithDashes() {
        assertTrue(CardValidator.isValidNumber("6037-9911-2233-4455"));
        assertTrue(CardValidator.isValidNumber("6037991122334455"));
    }

    @Test
    void rejectsWrongLengthOrNonNumericCard() {
        assertFalse(CardValidator.isValidNumber("6037-9911-2233"));       // too short
        assertFalse(CardValidator.isValidNumber("60379911223344556677")); // too long
        assertFalse(CardValidator.isValidNumber("6037-9911-2233-44AB"));  // non-numeric
        assertFalse(CardValidator.isValidNumber(null));
    }

    @Test
    void validatesCvv() {
        assertTrue(CardValidator.isValidCvv("123"));
        assertTrue(CardValidator.isValidCvv("1234"));
        assertFalse(CardValidator.isValidCvv("12"));
        assertFalse(CardValidator.isValidCvv("12a"));
        assertFalse(CardValidator.isValidCvv(null));
    }

    @Test
    void validatesExpiry() {
        assertTrue(CardValidator.isValidExpiry("01/27"));
        assertTrue(CardValidator.isValidExpiry("12/30"));
        assertFalse(CardValidator.isValidExpiry("13/27")); // month out of range
        assertFalse(CardValidator.isValidExpiry("00/27"));
        assertFalse(CardValidator.isValidExpiry("1/27"));
        assertFalse(CardValidator.isValidExpiry("2027-01"));
        assertFalse(CardValidator.isValidExpiry(null));
    }

    @Test
    void validatesHolder() {
        assertTrue(CardValidator.isValidHolder("Ada Lovelace"));
        assertFalse(CardValidator.isValidHolder("   "));
        assertFalse(CardValidator.isValidHolder(null));
    }

    @Test
    void fullCardIsValidOnlyWhenEveryFieldIsValid() {
        assertTrue(CardValidator.isValidCard("6037-9911-2233-4455", "Ada", "123", "01/27"));
        assertFalse(CardValidator.isValidCard("6037-9911", "Ada", "123", "01/27"));
        assertFalse(CardValidator.isValidCard("6037-9911-2233-4455", "", "123", "01/27"));
        assertFalse(CardValidator.isValidCard("6037-9911-2233-4455", "Ada", "12", "01/27"));
        assertFalse(CardValidator.isValidCard("6037-9911-2233-4455", "Ada", "123", "13/27"));
    }
}
