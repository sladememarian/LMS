package ir.ac.kntu.sso;

import ir.ac.kntu.exception.InvalidPasswordException;
import ir.ac.kntu.exception.InvalidPhoneFormatException;
import ir.ac.kntu.exception.InvalidThemeException;
import ir.ac.kntu.exception.UserNotFoundException;
import ir.ac.kntu.iam.IamService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.Validator;

public class SsoService {

    private static final String THEME_LIGHT = "LIGHT";
    private static final String THEME_DARK = "DARK";
    private static final String NO_PROFILE = "No profile found for ";

    public static String viewProfile(String email) {
        Persona persona = PersonaService.getProfile(email);
        if (persona == null) {
            throw new UserNotFoundException(NO_PROFILE + email);
        }
        StringBuilder summary = new StringBuilder();
        summary.append("Email: ").append(safe(persona.getEmail()));
        summary.append(" | Name: ").append(safe(persona.getFirstName()));
        summary.append(" | Family: ").append(safe(persona.getLastName()));
        summary.append(" | Phone: ").append(safe(persona.getPhoneNumber()));
        summary.append(" | Role: ").append(persona.getRole().name());
        summary.append(" | Theme: ").append(persona.getTheme());
        return summary.toString();
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }

    public static void editProfile(
        String email,
        String firstName,
        String lastName,
        String phoneNumber
    ) {
        Persona persona = PersonaService.getProfile(email);
        if (persona == null) {
            throw new UserNotFoundException(NO_PROFILE + email);
        }
        if (!Validator.isValidPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneFormatException(
                "Invalid phone number format"
            );
        }
        PersonaService.updateProfile(email, firstName, lastName, phoneNumber);
    }

    public static void changePassword(
        String email,
        String currentPassword,
        String newPassword,
        String confirm
    ) {
        if (!newPassword.equals(confirm)) {
            throw new InvalidPasswordException(
                "New password and confirmation do not match"
            );
        }
        IamService.changePassword(email, currentPassword, newPassword);
    }

    public static void changeTheme(String email, String theme) {
        String normalized = theme == null ? "" : theme.toUpperCase();
        if (!THEME_LIGHT.equals(normalized) && !THEME_DARK.equals(normalized)) {
            throw new InvalidThemeException("Theme must be LIGHT or DARK");
        }
        PersonaService.updateTheme(email, normalized);
    }

    public static String getTheme(String email) {
        Persona persona = PersonaService.getProfile(email);
        if (persona == null) {
            throw new UserNotFoundException(NO_PROFILE + email);
        }
        return persona.getTheme();
    }

    public static void logout() {
        SessionManager.destroySession();
    }
}
