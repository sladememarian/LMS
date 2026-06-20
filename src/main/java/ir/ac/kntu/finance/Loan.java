package ir.ac.kntu.finance;

/**
 * A single borrowed-item loan tracked against the simulated calendar. The due
 * day is expressed in simulated-day units (see {@link SimulationClock}); once
 * the simulated date passes it the item becomes overdue and a daily fine is
 * accrued by {@link LoanService}. {@code lastChargedDay} guards against charging
 * the same simulated day twice.
 */
public class Loan {

    private final String memberId;
    private final String itemId;
    private final int borrowDay;
    private int dueDay;
    private int lastChargedDay;

    public Loan(String memberId, String itemId, int borrowDay, int dueDay) {
        this.memberId = memberId;
        this.itemId = itemId;
        this.borrowDay = borrowDay;
        this.dueDay = dueDay;
        this.lastChargedDay = dueDay;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getBorrowDay() {
        return borrowDay;
    }

    public int getDueDay() {
        return dueDay;
    }

    public void setDueDay(int dueDay) {
        this.dueDay = dueDay;
    }

    public int getLastChargedDay() {
        return lastChargedDay;
    }

    public void setLastChargedDay(int lastChargedDay) {
        this.lastChargedDay = lastChargedDay;
    }
}
