package ir.ac.kntu.library;

public class EBook extends DigitalItem {

    private int pageCount;

    public EBook(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public String getItemType() {
        return "EBOOK";
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}
