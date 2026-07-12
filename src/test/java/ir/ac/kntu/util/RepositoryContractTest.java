package ir.ac.kntu.util;

import java.util.List;
import java.util.Optional;

import ir.ac.kntu.generic.Repository;
import ir.ac.kntu.library.SupplierCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryContractTest {

    private final Repository<SupplierCompany, String> repository = new SupplierRepository();

    @BeforeEach
    void clean() {
        SupplierRepository.clearSuppliers();
    }

    @Test
    void saveThenFindAllReturnsSavedItem() {
        repository.save(new SupplierCompany("SUP-100", "Generic Supplier"));

        List<SupplierCompany> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals("SUP-100", all.get(0).getCompanyId());
    }

    @Test
    void findByIdReturnsMatchingItem() {
        repository.save(new SupplierCompany("SUP-200", "Findable Supplier"));

        Optional<SupplierCompany> found = repository.findById("SUP-200");
        assertTrue(found.isPresent());
        assertEquals("Findable Supplier", found.get().getCompanyName());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertFalse(repository.findById("MISSING").isPresent());
    }

    @Test
    void deleteByIdRemovesItem() {
        repository.save(new SupplierCompany("SUP-300", "Removable Supplier"));
        repository.deleteById("SUP-300");

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void saveUpsertsExistingItemByNaturalKey() {
        repository.save(new SupplierCompany("SUP-400", "Original Name"));
        repository.save(new SupplierCompany("SUP-400", "Updated Name"));

        List<SupplierCompany> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals("Updated Name", all.get(0).getCompanyName());
    }
}
