package ir.ac.kntu.gui.util;

import java.util.regex.Pattern;

// GUI-side validation for the simulated bank-card top-up form. The phase-1/2
// backend never validated card details (the CLI used a loose "12+ digits,
// 3-digit CVV" check) and its Validator exposes no card regex, so to keep the
// backend untouched while rejecting obviously invalid input the real rules live
// here. They mirror the form's advertised format (6037-xxxx-xxxx-xxxx): a
// 16-digit PAN, a 3-4 digit CVV, a non-blank holder and an MM/YY expiry.
public final class CardValidator {

    private static final Pattern PAN_PATTERN = Pattern.compile("^\\d{16}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3,4}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/\\d{2}$");

    private CardValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // True when the card number is exactly 16 digits once dashes/spaces are stripped.
    public static boolean isValidNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        String digits = cardNumber.replace("-", "").replace(" ", "");
        return PAN_PATTERN.matcher(digits).matches();
    }

    // True when the CVV is 3 or 4 digits.
    public static boolean isValidCvv(String cvv) {
        return cvv != null && CVV_PATTERN.matcher(cvv.trim()).matches();
    }

    // True when the expiry is a well-formed MM/YY with month 01-12.
    public static boolean isValidExpiry(String expiry) {
        return expiry != null && EXPIRY_PATTERN.matcher(expiry.trim()).matches();
    }

    // True when the holder name is present (non-blank).
    public static boolean isValidHolder(String holder) {
        return holder != null && !holder.trim().isEmpty();
    }

    // Convenience: all four card fields are individually valid.
    public static boolean isValidCard(String cardNumber, String holder, String cvv, String expiry) {
        return isValidNumber(cardNumber)
                && isValidHolder(holder)
                && isValidCvv(cvv)
                && isValidExpiry(expiry);
    }
}
