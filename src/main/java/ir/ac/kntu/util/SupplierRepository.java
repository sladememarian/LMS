package ir.ac.kntu.util;

import java.util.List;
import java.util.Optional;

import ir.ac.kntu.generic.Repository;
import ir.ac.kntu.library.SupplierCompany;

// Persistence for the {@code suppliers} table. Split out of the former
// monolithic {@code DatabaseAccess} class as part of the per-domain
// repository migration. Suppliers have a single natural key, so this
// repository also implements the generic {@link Repository} abstraction.
public final class SupplierRepository implements Repository<SupplierCompany, String> {

    // Package-private: instantiated only to exercise the generic
    // {@link Repository} interface; static methods remain the primary
    // entry point for other classes.
    SupplierRepository() {
        // no-arg constructor documented per project style rules
    }

    public static void clearSuppliers() {
        Database.executeUpdate("DELETE FROM suppliers");
    }

    public static void insertSupplier(SupplierCompany supplier) {
        Database.withPs("MERGE INTO suppliers USING (VALUES (?, ?)) AS s(company_id, company_name) ON suppliers.company_id = s.company_id WHEN MATCHED THEN UPDATE SET company_name = s.company_name WHEN NOT MATCHED THEN INSERT (company_id, company_name) VALUES (s.company_id, s.company_name)", ps -> {
            ps.setString(1, supplier.getCompanyId());
            ps.setString(2, supplier.getCompanyName());
            ps.executeUpdate();
        });
    }

    public static List<SupplierCompany> getAllSuppliers() {
        return Database.queryAll("SELECT * FROM suppliers",
                rs -> new SupplierCompany(rs.getString("company_id"), rs.getString("company_name")));
    }

    public static void deleteSupplier(String companyId) {
        Database.withPs("DELETE FROM suppliers WHERE company_id=?", ps -> {
            ps.setString(1, companyId);
            ps.executeUpdate();
        });
    }

    @Override
    public List<SupplierCompany> findAll() {
        return getAllSuppliers();
    }

    @Override
    public Optional<SupplierCompany> findById(String id) {
        return getAllSuppliers().stream()
                .filter(s -> s.getCompanyId().equals(id))
                .findFirst();
    }

    @Override
    public void save(SupplierCompany item) {
        insertSupplier(item);
    }

    @Override
    public void deleteById(String id) {
        deleteSupplier(id);
    }
}
