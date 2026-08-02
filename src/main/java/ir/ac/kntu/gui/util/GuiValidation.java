package ir.ac.kntu.gui.util;

import ir.ac.kntu.util.Validator;

/**
 * Shared GUI input validation. Each method shows the appropriate warning dialog
 * itself and returns whether the input is acceptable, so form handlers read as a
 * single guard line instead of repeating the same warn-and-return chain.
 *
 * <p>Pure presentation: the actual rules come from the backend
 * {@link Validator}, which the CLI phases already define.
 */
public final class GuiValidation {

    private GuiValidation() {
    }

    /**
     * Validates a non-blank email/password pair: both must be present, the email
     * well-formed and the password strong enough (per {@link Validator}).
     *
     * @param email already-trimmed input
     * @param password raw input
     * @return true when both fields are acceptable
     */
    public static boolean requireValidEmailAndPassword(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Dialogs.warn("Missing information", "Email and password are required.");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            Dialogs.warn("Invalid email", "Please enter a valid email address.");
            return false;
        }
        if (!Validator.isValidPassword(password)) {
            Dialogs.warn("Weak password",
                    "Password must be at least 8 characters with uppercase, lowercase, digit, and special character.");
            return false;
        }
        return true;
    }
}
