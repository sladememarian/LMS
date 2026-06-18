package ir.ac.kntu.library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the admin/operator catalog operations added to LibraryService
 * (getItemById, addItem, deleteItem, updateItemPrice) and the expanded mock
 * data set.
 */
class LibraryAdminOpsTest {

    private Book sampleBook(String id) {
        Book book = new Book(id, "Temp Title", "Temp", 2020);
        book.setSupplierId("SUP-101");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setUnitPrice(100);
        book.setAuthor("Author");
        book.setIsbn("978-0000000000");
        return book;
    }

    @Test
    void getItemByIdFindsSeededItem() {
        assertNotNull(LibraryService.getItemById("ITEM-001"));
        assertNull(LibraryService.getItemById("NOPE-000"));
        assertNull(LibraryService.getItemById(null));
    }

    @Test
    void expandedMockDataIsSeeded() {
        // Inventory may be loaded from an existing library.enc (>= 5 baseline items)
        //  or freshly seeded (>= 11 expanded items). Either way it must be non-empty.
        assertFalse(LibraryService.getAllItems().isEmpty());
        assertTrue(LibraryService.getAllItems().size() >= 5);
    }

    @Test
    void addAndDeleteItem() {
        String id = "ITEM-T" + (System.nanoTime() % 100_000);
        assertTrue(LibraryService.addItem(sampleBook(id)));
        assertNotNull(LibraryService.getItemById(id));
        assertFalse(LibraryService.addItem(sampleBook(id)), "duplicate id rejected");
        assertTrue(LibraryService.deleteItem(id));
        assertNull(LibraryService.getItemById(id));
        assertFalse(LibraryService.deleteItem(id));
    }

    @Test
    void updatePrice() {
        String id = "ITEM-P" + (System.nanoTime() % 100_000);
        LibraryService.addItem(sampleBook(id));
        assertTrue(LibraryService.updateItemPrice(id, 555));
        assertFalse(LibraryService.updateItemPrice(id, -1));
        assertFalse(LibraryService.updateItemPrice("MISSING", 10));
    }
}
