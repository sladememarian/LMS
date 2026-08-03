package ir.ac.kntu.gui.util;

import ir.ac.kntu.util.Validator;

// Shared GUI input validation. Each method shows its own warning dialog and
// returns whether the input is valid, so form handlers can use a single guard
// line instead of repeating the same warn-and-return chain.
//
// Presentation only: the real rules come from the backend Validator, which the
// CLI phases already define.
public final class GuiValidation {

    private GuiValidation() {
        // utility class
    }

    // Checks an email/password pair: both must be present, the email must be
    // well-formed and the password strong enough (per Validator).
    // email is already trimmed; password is raw. Returns true when both are valid.
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
