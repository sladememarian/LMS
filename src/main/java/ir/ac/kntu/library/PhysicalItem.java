package ir.ac.kntu.library;

import ir.ac.kntu.interfaces.Reservable;

public abstract class PhysicalItem extends LibraryItem implements Reservable {
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
    // Reservable is implemented here (not on DigitalItem) since only physical
    // copies support a reservation queue. Magazine still overrides canReserve()
    // back to false below in its own class -- it's still "capable of being
    // reserved" in the type sense, it just always answers "no" today.

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