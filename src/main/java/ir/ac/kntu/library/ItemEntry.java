package ir.ac.kntu.library;

import java.util.Scanner;

import ir.ac.kntu.util.ConsoleMenu;

/**
 * Shared interactive builder for new catalog books, reused by the Admin and
 * CallCenter "Add Item" flows so the prompts are written only once.
 */
public class ItemEntry {

    private static final String CONDITION_GOOD = "GOOD";

    public static Book readNewBook(Scanner scanner) {
        String id = ConsoleMenu.readLine(scanner, "New Item ID: ");
        String title = ConsoleMenu.readLine(scanner, "Title: ");
        String category = ConsoleMenu.readLine(scanner, "Category: ");
        int year = ConsoleMenu.readInt(scanner, "Publish Year: ");
        Book book = new Book(id, title, category, year);
        configureCopies(scanner, book);
        book.setAuthor(ConsoleMenu.readLine(scanner, "Author: "));
        book.setIsbn(ConsoleMenu.readLine(scanner, "ISBN: "));
        book.setShelfLocation("Shelf NEW");
        book.setPhysicalCondition(CONDITION_GOOD);
        return book;
    }

    private static void configureCopies(Scanner scanner, Book book) {
        int total = ConsoleMenu.readInt(scanner, "Total Copies: ");
        int price = ConsoleMenu.readInt(scanner, "Unit Price: ");
        book.setSupplierId(ConsoleMenu.readLine(scanner, "Supplier ID (SUP-101..104): "));
        book.setTotalCopies(Math.max(total, 0));
        book.setAvailableCopies(Math.max(total, 0));
        book.setUnitPrice(Math.max(price, 0));
    }
}
