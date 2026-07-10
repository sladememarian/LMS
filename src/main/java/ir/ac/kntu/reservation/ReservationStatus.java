package ir.ac.kntu.reservation;

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
        for (ReservationStatus s : values()) {
            if (s.label.equalsIgnoreCase(label)) {
                return s;
            }
        }
        return WAITING;
    }
}
