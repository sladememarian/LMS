package ir.ac.kntu.gui.signup;

import java.util.HashMap;
import java.util.Map;

// Immutable envelope carrying the profile fields collected at sign-up that are
// NOT part of the fast account-creation path (email + password). It's the
// message pushed onto SignupQueue and later applied by SignupWorker via
// PersonaService.updateProfile. Serialized as one JSON line so the queue can be
// spooled to disk (one envelope per line) for durability and crash recovery.
public final class SignupEnvelope {

    private static final String KEY_EMAIL = "email";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_PHONE = "phoneNumber";

    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;

    public SignupEnvelope(String email, String firstName, String lastName, String phoneNumber) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    // Renders this envelope as one JSON object on a single line.
    public String toJsonLine() {
        return "{"
                + jsonField(KEY_EMAIL, email) + ","
                + jsonField(KEY_FIRST_NAME, firstName) + ","
                + jsonField(KEY_LAST_NAME, lastName) + ","
                + jsonField(KEY_PHONE, phoneNumber)
                + "}";
    }

    private static String jsonField(String key, String value) {
        return "\"" + key + "\":\"" + escape(value) + "\"";
    }

    // Parses a line produced by toJsonLine(). Returns null if malformed.
    public static SignupEnvelope fromJsonLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        Map<String, String> fields = parseFlatJson(line);
        if (!fields.containsKey(KEY_EMAIL)) {
            return null;
        }
        return new SignupEnvelope(
                fields.get(KEY_EMAIL),
                fields.getOrDefault(KEY_FIRST_NAME, ""),
                fields.getOrDefault(KEY_LAST_NAME, ""),
                fields.getOrDefault(KEY_PHONE, ""));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        // Control-flow loop: a char-by-char JSON escaper; each character maps to a
        // variable-length replacement, so it isn't a plain collection transform.
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\\') {
                sb.append("\\\\");
            } else if (ch == '"') {
                sb.append("\\\"");
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch == '\t') {
                sb.append("\\t");
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    // Minimal parser for a flat {"k":"v",...} object with string values only.
    private static Map<String, String> parseFlatJson(String line) {
        Map<String, String> out = new HashMap<>();
        int cursor = 0;
        int length = line.length();
        // Control-flow loop: a hand-rolled scanning parser that advances a cursor
        // through the line; stateful position tracking, not a stream pipeline.
        while (cursor < length) {
            int keyStart = line.indexOf('"', cursor);
            if (keyStart < 0) {
                break;
            }
            StringBuilder key = new StringBuilder();
            cursor = readString(line, keyStart + 1, key);
            int colon = line.indexOf(':', cursor);
            if (colon < 0) {
                break;
            }
            int valStart = line.indexOf('"', colon);
            if (valStart < 0) {
                break;
            }
            StringBuilder val = new StringBuilder();
            cursor = readString(line, valStart + 1, val);
            out.put(key.toString(), val.toString());
        }
        return out;
    }

    // Reads a JSON string starting just after the opening quote at index `from`,
    // appends the unescaped contents to `sink`, returns the index after the close quote.
    private static int readString(String line, int from, StringBuilder sink) {
        int index = from;
        // Control-flow loop: consumes an escaped JSON string char-by-char until
        // the closing quote; index jumps by 2 on escapes, so no stream fits.
        while (index < line.length()) {
            char ch = line.charAt(index);
            if (ch == '\\' && index + 1 < line.length()) {
                sink.append(unescape(line.charAt(index + 1)));
                index += 2;
            } else if (ch == '"') {
                return index + 1;
            } else {
                sink.append(ch);
                index++;
            }
        }
        return index;
    }

    private static char unescape(char escaped) {
        if (escaped == 'n') {
            return '\n';
        }
        if (escaped == 'r') {
            return '\r';
        }
        if (escaped == 't') {
            return '\t';
        }
        return escaped;
    }
}
