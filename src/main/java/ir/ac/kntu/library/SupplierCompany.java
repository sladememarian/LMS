package ir.ac.kntu.library;

public class SupplierCompany {
    // suppliers: the unsung heroes of the book world
    private final String companyId;
    private final String companyName;

    public SupplierCompany(String id, String name) {
        this.companyId = id;
        this.companyName = name;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }
}