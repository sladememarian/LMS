package ir.ac.kntu.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The validation rules that guard registration and catalog input.
 */
class ValidatorTest {

    @Test
    void emailValidation() {
        assertTrue(Validator.isValidEmail("user.name@example.com"));
        assertFalse(Validator.isValidEmail("no-at-symbol"));
        assertFalse(Validator.isValidEmail(null));
    }

    @Test
    void passwordPolicy() {
        assertTrue(Validator.isValidPassword("Passw0rd!"));
        assertFalse(Validator.isValidPassword("short1!"));
        assertFalse(Validator.isValidPassword("NoSpecial123"));
        assertFalse(Validator.isValidPassword(null));
    }

    @Test
    void identifierValidation() {
        assertTrue(Validator.isValidMemberId("STU-123456"));
        assertFalse(Validator.isValidMemberId("XYZ-123456"));
        assertTrue(Validator.isValidItemId("BOK-12345678"));
        assertFalse(Validator.isValidItemId("BOK-1234"));
    }
}
