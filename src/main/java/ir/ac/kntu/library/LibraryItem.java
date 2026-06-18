package ir.ac.kntu.library;

public abstract class LibraryItem {

    private final String itemId;
    private final String title;
    private final String category;
    private final int publishYear;
    private String supplierId;
    private int totalCopies;
    private int availableCopies;
    private int unitPrice;

    public LibraryItem(String id, String title, String cat, int year) {
        this.itemId = id;
        this.title = title;
        this.category = cat;
        this.publishYear = year;
    }

    public abstract String getItemType();

    public String getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public int getPublishYear() {
        return publishYear;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getBorrowedCopies() {
        return totalCopies - availableCopies;
    }
}
