package ir.ac.kntu.library;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Core Library catalog behaviour: seeding, search, and borrow/return
 * availability accounting.
 */
class LibraryServiceTest {

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
        List<LibraryItem> items = LibraryService.getAllItems();
        assertFalse(items.isEmpty(), "Inventory should be seeded");
    }

    @Test
    void searchHandlesKeywordAndBlankInput() {
        assertFalse(LibraryService.searchItems("clean").isEmpty());
        assertTrue(LibraryService.searchItems("").isEmpty());
        assertTrue(LibraryService.searchItems(null).isEmpty());
    }

    @Test
    void borrowAndReturnAdjustsAvailability() {
        String id = LibraryService.getAllItems().get(0).getItemId();
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
}
