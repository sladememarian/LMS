package ir.ac.kntu.library;

import java.util.Scanner;

import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.Validator;

public class ItemEntry {
    private static final String CONDITION_GOOD = "GOOD";

    private ItemEntry() {
    }

    public static LibraryItem readNewItem(Scanner scanner) {
        System.out.println(
                "Item types: 1. Book  2. EBook  3. AudioBook  4. Magazine");
        String type = ConsoleMenu.readLine(scanner, "Type: ");
        ItemHeader header = readHeader(scanner);
        switch (type) {
            case "1":
                return readBook(scanner, header);
            case "2":
                return readEBook(scanner, header);
            case "3":
                return readAudioBook(scanner, header);
            case "4":
                return readMagazine(scanner, header);
            default:
                ConsoleColor.printError("Unknown type. Defaulting to Book.");
                return readBook(scanner, header);
        }
    }

    private static ItemHeader readHeader(Scanner scanner) {
        String id = ConsoleMenu.readLine(scanner, "New Item ID: ");
        String title = ConsoleMenu.readLine(scanner, "Title: ");
        String category = ConsoleMenu.readLine(scanner, "Category: ");
        int year = ConsoleMenu.readInt(scanner, "Publish Year: ");
        return new ItemHeader(id, title, category, year);
    }

    private static Book readBook(Scanner scanner, ItemHeader header) {
        Book book = new Book(header.id(), header.title(),
                header.category(), header.year());
        configurePhysical(scanner, book);
        book.setAuthor(ConsoleMenu.readLine(scanner, "Author: "));
        String isbn;
        while (true) {
            isbn = ConsoleMenu.readLine(scanner,
                    "ISBN-13 (or 0 to skip): ");
            if ("0".equals(isbn) || Validator.isValidISBN13(isbn)) {
                break;
            }
            ConsoleColor.printError(
                    "Invalid ISBN-13. Must be 13 digits starting with 978 or 979.");
        }
        book.setIsbn(isbn);
        return book;
    }

    private static EBook readEBook(Scanner scanner, ItemHeader header) {
        EBook ebook = new EBook(header.id(), header.title(),
                header.category(), header.year());
        configureDigital(scanner, ebook);
        ebook.setPageCount(ConsoleMenu.readInt(scanner, "Page Count: "));
        return ebook;
    }

    private static AudioBook readAudioBook(Scanner scanner,
            ItemHeader header) {
        AudioBook audio = new AudioBook(header.id(), header.title(),
                header.category(), header.year());
        configureDigital(scanner, audio);
        audio.setNarrator(ConsoleMenu.readLine(scanner, "Narrator: "));
        audio.setDurationMinutes(
                ConsoleMenu.readInt(scanner, "Duration (minutes): "));
        return audio;
    }

    private static Magazine readMagazine(Scanner scanner,
            ItemHeader header) {
        Magazine mag = new Magazine(header.id(), header.title(),
                header.category(), header.year());
        configurePhysical(scanner, mag);
        mag.setIssueNumber(
                ConsoleMenu.readInt(scanner, "Issue Number: "));
        return mag;
    }

    private static void configurePhysical(Scanner scanner,
            PhysicalItem item) {
        int total = ConsoleMenu.readInt(scanner, "Total Copies: ");
        int price = ConsoleMenu.readInt(scanner, "Unit Price: ");
        item.setSupplierId(ConsoleMenu.readLine(scanner,
                "Supplier ID (SUP-101..104): "));
        item.setTotalCopies(Math.max(total, 0));
        item.setAvailableCopies(Math.max(total, 0));
        item.setUnitPrice(Math.max(price, 0));
        item.setShelfLocation(
                ConsoleMenu.readLine(scanner, "Shelf Location: "));
        item.setPhysicalCondition(CONDITION_GOOD);
    }

    private static void configureDigital(Scanner scanner,
            DigitalItem item) {
        int total = ConsoleMenu.readInt(scanner, "Total Copies: ");
        int price = ConsoleMenu.readInt(scanner, "Unit Price: ");
        item.setSupplierId(ConsoleMenu.readLine(scanner,
                "Supplier ID (SUP-101..104): "));
        item.setTotalCopies(Math.max(total, 0));
        item.setAvailableCopies(Math.max(total, 0));
        item.setUnitPrice(Math.max(price, 0));
        item.setDownloadUrl(ConsoleMenu.readLine(scanner,
                "Download URL: "));
        item.setFileSize(ConsoleMenu.readLong(scanner,
                "File Size (bytes): "));
    }

    private record ItemHeader(String id, String title,
            String category, int year) {
    }
}
