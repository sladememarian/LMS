package ir.ac.kntu.finance;

public class Loan {
    // a loan: where books and deadlines meet
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

    public boolean isOverdue(int currentDay) {
        return currentDay > dueDay;
    }

    public int daysOverdue(int currentDay) {
        return Math.max(0, currentDay - dueDay);
    }
}