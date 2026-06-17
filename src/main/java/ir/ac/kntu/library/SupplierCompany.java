package ir.ac.kntu.library;

public class SupplierCompany {

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
