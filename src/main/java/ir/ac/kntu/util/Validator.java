package ir.ac.kntu.util;

import java.time.Year;
import java.util.regex.Pattern;

public final class Validator {

    private Validator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    //email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    // Starts with +989, 989, or 09 - 9digits
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+98|98|0)9\\d{9}$");

    //prefix must be FAC, STU, or GST - 6digitts
    private static final Pattern MEMBER_ID_PATTERN = Pattern.compile("^(FAC|STU|GST)-\\d{6}$");

    //prefix must be BOK, MAG, EBK, or AUD
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("^(BOK|MAG|EBK|AUD)-\\d{8}$");

    private static final Pattern ISBN13_PATTERN = Pattern.compile("^(978|979)\\d{10}$");
    private static final Pattern ISSN_PATTERN = Pattern.compile("^\\d{4}-\\d{3}[0-9X]$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        String specialChars = "!@#$%^&*()-_=+[{]};:'\",<.>/?~`|\\";

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true; 
            }else if (Character.isLowerCase(c)) {
                hasLower = true; 
            }else if (Character.isDigit(c)) {
                hasDigit = true; 
            }else if (specialChars.contains(String.valueOf(c))) {
                hasSpecial = true;
            }
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static boolean isValidMemberId(String memberId) {
        return memberId != null && MEMBER_ID_PATTERN.matcher(memberId).matches();
    }

    public static boolean isValidItemId(String itemId) {
        return itemId != null && ITEM_ID_PATTERN.matcher(itemId).matches();
    }

    public static boolean isValidISBN13(String isbn) {
        return isbn != null && ISBN13_PATTERN.matcher(isbn).matches();
    }

    public static boolean isValidISSN(String issn) {
        return issn != null && ISSN_PATTERN.matcher(issn).matches();
    }

    public static boolean isValidPublishYear(int year) {
        int currentYear = Year.now().getValue();
        return year >= 1450 && year <= currentYear;
    }

    public static boolean isValidDownloadUrl(String url) {
        return url != null && url.startsWith("https://");
    }
}
