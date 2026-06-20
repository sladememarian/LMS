package ir.ac.kntu.persona;

public enum UserRole {
    // ADMIN can borrow the whole library, hope they have a big bag
    ADMIN(Integer.MAX_VALUE, "ADM-"),
    CALLCENTER(0, "CC-"),
    TEACHER(15, "FAC-"),
    STUDENT(10, "STU-"),
    GUEST(2, "GST-");

    private final int maxBorrowLimit;
    private final String prefix;

    UserRole(int maxBorrowLimit, String prefix) {
        this.maxBorrowLimit = maxBorrowLimit;
        this.prefix = prefix;
    }

    public int getMaxBorrowLimit() {
        return maxBorrowLimit;
    }

    public String getPrefix() {
        return prefix;
    }
}