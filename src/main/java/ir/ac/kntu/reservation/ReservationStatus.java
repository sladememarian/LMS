package ir.ac.kntu.reservation;

import java.util.Arrays;

public enum ReservationStatus {
    WAITING("WAITING"),
    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED"),
    EXPIRED("EXPIRED"),
    CANCELLED("CANCELLED");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ReservationStatus fromLabel(String label) {
        return Arrays.stream(values())
                .filter(s -> s.label.equalsIgnoreCase(label))
                .findFirst()
                .orElse(WAITING);
    }
}
