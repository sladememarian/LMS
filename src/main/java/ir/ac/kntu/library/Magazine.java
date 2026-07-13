package ir.ac.kntu.library;

import java.util.List;

public class Magazine extends PhysicalItem {
    // periodicals: because news doesn't wait for your attention span
    private static final int BORROW_PERIOD_DAYS = 7;

    private int issueNumber;

    public Magazine(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public String getItemType() {
        return "MAGAZINE";
    }

    @Override
    public boolean canReserve() {
        return true;
    }

    @Override
    public int borrowPeriod() {
        return BORROW_PERIOD_DAYS;
    }

    @Override
    public String displayInfo() {
        return "Issue #" + issueNumber;
    }

    @Override
    public List<String> availableActions() {
        return baseActions();
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }
}