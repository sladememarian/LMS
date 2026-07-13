package ir.ac.kntu.library;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.interfaces.Borrowable;
import ir.ac.kntu.interfaces.Searchable;

public abstract class LibraryItem implements Borrowable, Searchable {

    private final String itemId;
    private final String title;
    private final String category;
    private final int publishYear;
    private String supplierId;
    private int totalCopies;
    private int availableCopies;
    private int unitPrice;

    public LibraryItem(String id, String title, String cat, int year) {
        // an item without a title is just a... well, nothing
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

    // Whether stock allows another borrow right now. This is per-instance DATA
    // (how many copies are left), not per-type behavior, so unlike the methods
    // below it is intentionally NOT abstract/overridden -- see docs/step4.md.
    @Override
    public boolean canBorrow() {
        return availableCopies > 0;
    }

    // Whether a search keyword matches this item's title or category.
    @Override
    public boolean matchesQuery(String query) {
        return matchedField(query) != null;
    }

    // Which field a search keyword matched ("title"/"category"), or null if none.
    public String matchedField(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        String cleanQuery = query.toLowerCase().trim();
        if (title.toLowerCase().contains(cleanQuery)) {
            return "title";
        }
        if (category.toLowerCase().contains(cleanQuery)) {
            return "category";
        }
        return null;
    }

    // Whether this item type supports being placed on a reservation queue.
    public abstract boolean canReserve();

    // Loan length in days for this item type.
    public abstract int borrowPeriod();

    // One-line, type-specific detail (e.g. author, narrator, page count).
    public abstract String displayInfo();

    // Actions a member can perform on this item, tailored to its type.
    public abstract List<String> availableActions();

    protected List<String> baseActions() {
        List<String> actions = new ArrayList<>();
        actions.add("Borrow");
        actions.add("Return");
        if (canReserve()) {
            actions.add("Reserve");
        }
        return actions;
    }
}
