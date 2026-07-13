package ir.ac.kntu.mail;

import java.util.Arrays;

public enum MessageType {
    TWO_FA("2FA"),
    WELCOME("WELCOME"),
    PASSWORD_RESET("PASSWORD_RESET"),
    SYSTEM_NOTIFICATION("SYSTEM_NOTIFICATION");

    private final String label;

    MessageType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static MessageType fromLabel(String value) {
        return Arrays.stream(values())
                .filter(t -> t.label.equals(value))
                .findFirst()
                .orElse(SYSTEM_NOTIFICATION);
    }
}