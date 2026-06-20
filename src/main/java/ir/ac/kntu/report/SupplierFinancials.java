package ir.ac.kntu.report;

import ir.ac.kntu.library.LibraryItem;

public class SupplierFinancials {
    // tracking the money we owe to book suppliers
    private final String companyId;
    private final String companyName;
    private int itemCount;
    private int totalCopies;
    private int borrowedCopies;
    private long inventoryValue;
    private long circulationValue;

    public SupplierFinancials(String companyId, String companyName) {
        this.companyId = companyId;
        this.companyName = companyName;
    }

    public void accumulate(LibraryItem item) {
        itemCount++;
        totalCopies += item.getTotalCopies();
        borrowedCopies += item.getBorrowedCopies();
        inventoryValue += (long) item.getUnitPrice() * item.getTotalCopies();
        circulationValue += (long) item.getUnitPrice() * item.getBorrowedCopies();
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getBorrowedCopies() {
        return borrowedCopies;
    }

    public long getInventoryValue() {
        return inventoryValue;
    }

    public long getCirculationValue() {
        return circulationValue;
    }
}