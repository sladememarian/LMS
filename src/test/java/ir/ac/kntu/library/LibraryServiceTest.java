package ir.ac.kntu.library;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServiceTest {

    @Test
    void inventorySeededAndSuppliersAligned() {
        List<LibraryItem> items = LibraryService.getAllItems();
        assertFalse(items.isEmpty(), "Inventory should be seeded");
        List<SupplierCompany> suppliers = LibraryService.getAllSuppliers();
        assertEquals(4, suppliers.size());
        for (LibraryItem item : items) {
            String name = LibraryService.getSupplierName(item.getSupplierId());
            assertFalse(name.isEmpty());
        }
    }

    @Test
    void supplierNameLookup() {
        assertEquals("Global Books Inc.", LibraryService.getSupplierName("SUP-101"));
        assertEquals("UNKNOWN-ID", LibraryService.getSupplierName("UNKNOWN-ID"));
    }

    @Test
    void searchMatchesTitleOrCategory() {
        assertFalse(LibraryService.searchItems("clean").isEmpty());
        assertTrue(LibraryService.searchItems("").isEmpty());
        assertTrue(LibraryService.searchItems(null).isEmpty());
    }

    @Test
    void borrowAndReturnAdjustsAvailability() {
        LibraryItem item = LibraryService.getAllItems().get(0);
        String id = item.getItemId();
        int before = findById(id).getAvailableCopies();
        assertTrue(LibraryService.executeBorrow(id));
        assertEquals(before - 1, findById(id).getAvailableCopies());
        LibraryService.executeReturn(id);
        assertEquals(before, findById(id).getAvailableCopies());
    }

    @Test
    void borrowUnknownItemFails() {
        assertFalse(LibraryService.executeBorrow("ITEM-DOES-NOT-EXIST"));
    }

    @Test
    void callCenterStockUpdateIncreasesCopies() {
        LibraryItem item = LibraryService.getAllItems().get(0);
        String id = item.getItemId();
        int before = findById(id).getTotalCopies();
        LibraryService.updateItemQuantityFromCallCenter(id, 3);
        assertEquals(before + 3, findById(id).getTotalCopies());
    }

    @Test
    void pricingAndBorrowedDerivation() {
        for (LibraryItem item : LibraryService.getAllItems()) {
            assertEquals(item.getTotalCopies() - item.getAvailableCopies(), item.getBorrowedCopies());
            assertTrue(item.getUnitPrice() >= 0);
        }
    }

    private LibraryItem findById(String id) {
        for (LibraryItem item : LibraryService.getAllItems()) {
            if (item.getItemId().equals(id)) {
                return item;
            }
        }
        throw new IllegalStateException("missing " + id);
    }
}
