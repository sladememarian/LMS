package ir.ac.kntu.library;

import ir.ac.kntu.exception.BaseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LibraryAdminOpsTest {
    // Admin ops: the god-mode of library management

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
        // ITEM-001: the oldest item in the west
        assertNotNull(LibraryService.getItemById("ITEM-001"));
        assertNull(LibraryService.getItemById("NOPE-000"));
        assertNull(LibraryService.getItemById(null));
    }

    @Test
    void expandedMockDataIsSeeded() {
        // 5 items minimum: because 4 looked too empty
        assert(!LibraryService.getAllItems().isEmpty());
        assert(LibraryService.getAllItems().size() >= 5);
    }

    @Test
    void addAndDeleteItem() {
        // Add, find, reject duplicate, delete. The circle of library life.
        String id = "ITEM-T" + (System.nanoTime() % 100_000);
        assertDoesNotThrow(() -> LibraryService.addItem(sampleBook(id)));
        assertNotNull(LibraryService.getItemById(id));
        assertThrows(BaseException.class, () -> LibraryService.addItem(sampleBook(id)));
        assertDoesNotThrow(() -> LibraryService.deleteItem(id));
        assertNull(LibraryService.getItemById(id));
        assertThrows(BaseException.class, () -> LibraryService.deleteItem(id));
    }

    @Test
    void updatePrice() {
        // Price goes up: inflation simulator 2026
        String id = "ITEM-P" + (System.nanoTime() % 100_000);
        LibraryService.addItem(sampleBook(id));
        assertDoesNotThrow(() -> LibraryService.updateItemPrice(id, 555));
        assertThrows(BaseException.class, () -> LibraryService.updateItemPrice(id, -1));
        assertThrows(BaseException.class, () -> LibraryService.updateItemPrice("MISSING", 10));
    }
}