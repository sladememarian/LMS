package ir.ac.kntu.library;

import ir.ac.kntu.exception.BaseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServiceTest {
    // Library: where books go to be "borrowed" and never returned

    private LibraryItem findById(String id) {
        for (LibraryItem item : LibraryService.getAllItems()) {
            if (item.getItemId().equals(id)) {
                return item;
            }
        }
        throw new IllegalStateException("missing " + id);
    }

    @Test
    void inventoryIsSeeded() {
        // Seeded inventory: the library equivalent of "it's not much but it's honest work"
        List<LibraryItem> items = LibraryService.getAllItems();
        assertTrue(!items.isEmpty(), "Inventory should be seeded");
    }

    @Test
    void searchHandlesKeywordAndBlankInput() {
        // Searching "" returns nothing. Shocking.
        assertTrue(!LibraryService.searchItems("clean").isEmpty());
        assertTrue(LibraryService.searchItems("").isEmpty());
        assertTrue(LibraryService.searchItems(null).isEmpty());
    }

    @Test
    void borrowAndReturnAdjustsAvailability() {
        // Borrow one, minus one. Return one, plus one. Math checks out.
        String id = LibraryService.getAllItems().get(0).getItemId();
        int before = findById(id).getAvailableCopies();
        assertDoesNotThrow(() -> LibraryService.executeBorrow(id));
        assertEquals(before - 1, findById(id).getAvailableCopies());
        LibraryService.executeReturn(id);
        assertEquals(before, findById(id).getAvailableCopies());
    }

    @Test
    void borrowUnknownItemFails() {
        // "ITEM-DOES-NOT-EXIST" - no, really?
        assertThrows(BaseException.class, () -> LibraryService.executeBorrow("ITEM-DOES-NOT-EXIST"));
    }
}