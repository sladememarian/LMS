package ir.ac.kntu.util;

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Test
    void emailValidation() {
        assertTrue(Validator.isValidEmail("user.name@example.com"));
        assertTrue(Validator.isValidEmail("a@b.io"));
        assertFalse(Validator.isValidEmail("no-at-symbol"));
        assertFalse(Validator.isValidEmail("missing@domain"));
        assertFalse(Validator.isValidEmail(null));
    }

    @Test
    void phoneValidation() {
        assertTrue(Validator.isValidPhoneNumber("09123456789"));
        assertTrue(Validator.isValidPhoneNumber("989123456789"));
        assertTrue(Validator.isValidPhoneNumber("+989123456789"));
        assertFalse(Validator.isValidPhoneNumber("12345"));
        assertFalse(Validator.isValidPhoneNumber(null));
    }

    @Test
    void passwordPolicy() {
        assertTrue(Validator.isValidPassword("Passw0rd!"));
        assertFalse(Validator.isValidPassword("short1!"));
        assertFalse(Validator.isValidPassword("alllowercase1!"));
        assertFalse(Validator.isValidPassword("NoSpecial123"));
        assertFalse(Validator.isValidPassword(null));
    }

    @Test
    void idValidation() {
        assertTrue(Validator.isValidMemberId("STU-123456"));
        assertTrue(Validator.isValidMemberId("FAC-654321"));
        assertFalse(Validator.isValidMemberId("XYZ-123456"));
        assertTrue(Validator.isValidItemId("BOK-12345678"));
        assertFalse(Validator.isValidItemId("BOK-1234"));
    }

    @Test
    void publicationValidation() {
        assertTrue(Validator.isValidISBN13("9780132350884"));
        assertFalse(Validator.isValidISBN13("123"));
        assertTrue(Validator.isValidISSN("1234-567X"));
        assertFalse(Validator.isValidISSN("12345678"));
        assertTrue(Validator.isValidPublishYear(2000));
        assertTrue(Validator.isValidPublishYear(Year.now().getValue()));
        assertFalse(Validator.isValidPublishYear(1000));
    }

    @Test
    void downloadUrlValidation() {
        assertTrue(Validator.isValidDownloadUrl("https://kntu.ac/x"));
        assertFalse(Validator.isValidDownloadUrl("http://insecure"));
        assertFalse(Validator.isValidDownloadUrl(null));
    }
}
