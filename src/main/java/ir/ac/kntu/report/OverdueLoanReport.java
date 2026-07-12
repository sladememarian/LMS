package ir.ac.kntu.report;

import ir.ac.kntu.interfaces.Displayable;

public class OverdueLoanReport implements Displayable {
    // one overdue loan, as of whatever day SimulationClock says it is right now
    private final String memberId;
    private final String itemId;
    private final int dueDay;
    private final int daysOverdue;
    private final int projectedFine;

    public OverdueLoanReport(String memberId, String itemId, int dueDay, int daysOverdue, int projectedFine) {
        this.memberId = memberId;
        this.itemId = itemId;
        this.dueDay = dueDay;
        this.daysOverdue = daysOverdue;
        this.projectedFine = projectedFine;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getDueDay() {
        return dueDay;
    }

    public int getDaysOverdue() {
        return daysOverdue;
    }

    public int getProjectedFine() {
        return projectedFine;
    }

    @Override
    public String toDisplayString() {
        return "  " + memberId + " | Item: " + itemId + " | Due day " + dueDay
                + " | " + daysOverdue + " day(s) overdue | Fine so far: " + projectedFine;
    }
}
