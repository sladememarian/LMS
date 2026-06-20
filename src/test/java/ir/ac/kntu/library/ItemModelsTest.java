package ir.ac.kntu.library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModelsTest {
    // Item models: a taxonomy of stuff you can borrow

    @Test
    void bookIsPhysicalWithMetadata() {
        // The kind you can throw at someone
        Book book = new Book("BOK-1", "Title", "Cat", 2010);
        book.setAuthor("Author");
        book.setIsbn("9780132350884");
        book.setTotalCopies(10);
        book.setAvailableCopies(4);
        book.setUnitPrice(50);
        assertEquals("BOOK", book.getItemType());
        assertTrue(book instanceof PhysicalItem);
        assertEquals("Author", book.getAuthor());
        assertEquals(6, book.getBorrowedCopies());
        assertEquals(50, book.getUnitPrice());
    }

    @Test
    void ebookIsDigital() {
        // The kind that lives in the cloud and DRM
        EBook ebook = new EBook("EBK-1", "T", "C", 2020);
        ebook.setDownloadUrl("https://x");
        ebook.setFileSize(1_000L);
        ebook.setPageCount(123);
        assertEquals("EBOOK", ebook.getItemType());
        assertTrue(ebook instanceof DigitalItem);
        assertEquals(123, ebook.getPageCount());
    }

    @Test
    void magazineAndAudioBook() {
        // For people who read AND people who "read"
        Magazine magazine = new Magazine("MAG-1", "T", "C", 2021);
        magazine.setIssueNumber(7);
        assertEquals("MAGAZINE", magazine.getItemType());
        assertEquals(7, magazine.getIssueNumber());

        AudioBook audio = new AudioBook("AUD-1", "T", "C", 2019);
        audio.setNarrator("Voice");
        audio.setDurationMinutes(45);
        assertEquals("AUDIOBOOK", audio.getItemType());
        assertEquals("Voice", audio.getNarrator());
        assertEquals(45, audio.getDurationMinutes());
    }

    @Test
    void supplierCompanyGetters() {
        // SUP-900: over 9000 units supplied
        SupplierCompany supplier = new SupplierCompany("SUP-900", "Acme");
        assertEquals("SUP-900", supplier.getCompanyId());
        assertEquals("Acme", supplier.getCompanyName());
    }
}