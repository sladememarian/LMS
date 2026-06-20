package ir.ac.kntu.sso;

import ir.ac.kntu.iam.IamService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;

import ir.ac.kntu.util.Validator;

public class SsoService {
    // the S in SSO stands for 'Seriously Secure' (not really)
    private static final String THEME_LIGHT = "LIGHT";
    private static final String THEME_DARK = "DARK";

    public static String viewProfile(String email) {
        Persona persona = PersonaService.getProfile(email);
        if (persona == null) {
            throw new IllegalArgumentException("No profile found for " + email);
        }
        return "ID: " + persona.getMemberId()
                + " | Name: " + safe(persona.getFirstName())
                + " | Family: " + safe(persona.getLastName())
                + " | Email: " + safe(persona.getEmail())
                + " | Phone: " + safe(persona.getPhoneNumber())
                + " | Role: " + persona.getRole().name()
                + " | Theme: " + persona.getTheme();
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }

    public static void editProfile(String email, String firstName, String lastName, String phoneNumber) {
        Persona persona = PersonaService.getProfile(email);
        if (persona == null) {
            throw new IllegalArgumentException("No profile found for " + email);
        }
        if (!Validator.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        PersonaService.updateProfile(email, firstName, lastName, phoneNumber);
    }

    public static boolean changePassword(String email, String currentPassword, String newPassword, String confirm) {
        if (!newPassword.equals(confirm)) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        return IamService.changePassword(email, currentPassword, newPassword);
    }

    public static void changeTheme(String email, String theme) {
        String normalized = theme == null ? "" : theme.toUpperCase();
        if (!THEME_LIGHT.equals(normalized) && !THEME_DARK.equals(normalized)) {
            throw new IllegalArgumentException("Theme must be LIGHT or DARK");
        }
        PersonaService.updateTheme(email, normalized);
    }

    public static String getTheme(String email) {
        Persona persona = PersonaService.getProfile(email);
        return persona == null ? THEME_LIGHT : persona.getTheme();
    }

    public static void logout() {
        SessionManager.destroySession();
    }
}