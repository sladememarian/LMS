package ir.ac.kntu.mail;

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
        for (MessageType type : values()) {
            if (type.label.equals(value)) {
                return type;
            }
        }
        return SYSTEM_NOTIFICATION;
    }
}
