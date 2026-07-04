package ir.ac.kntu.library;

import java.util.List;

public class EBook extends DigitalItem {
    // it's a book but also not a book - welcome to the future
    private static final int BORROW_PERIOD_DAYS = 21;

    private int pageCount;

    public EBook(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public String getItemType() {
        return "EBOOK";
    }

    @Override
    public int borrowPeriod() {
        return BORROW_PERIOD_DAYS;
    }

    @Override
    public String displayInfo() {
        return "Pages: " + pageCount;
    }

    @Override
    public List<String> availableActions() {
        List<String> actions = baseActions();
        actions.add("Download");
        return actions;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}