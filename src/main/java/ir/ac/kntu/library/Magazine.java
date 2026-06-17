package ir.ac.kntu.library;

public class Magazine extends PhysicalItem {

    private int issueNumber;

    public Magazine(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public String getItemType() {
        return "MAGAZINE";
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }
}
