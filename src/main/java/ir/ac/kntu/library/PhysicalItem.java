package ir.ac.kntu.library;

public abstract class PhysicalItem extends LibraryItem {
    // physical things wear out, unlike this comment
    @Override
    public String getItemType() {
        return "PHYSICAL";
    }

    private String shelfLocation;
    private String physicalCondition;

    public PhysicalItem(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public boolean canReserve() {
        return true;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public String getPhysicalCondition() {
        return physicalCondition;
    }

    public void setPhysicalCondition(String condition) {
        this.physicalCondition = condition;
    }
}