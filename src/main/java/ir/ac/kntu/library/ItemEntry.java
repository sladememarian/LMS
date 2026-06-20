package ir.ac.kntu.library;

import java.util.Scanner;

import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.Validator;

public class ItemEntry {
    // reading book details from stdin, one field at a time
    private static final String CONDITION_GOOD = "GOOD";

    public static Book readNewBook(Scanner scanner) {
        String id = ConsoleMenu.readLine(scanner, "New Item ID: ");
        String title = ConsoleMenu.readLine(scanner, "Title: ");
        String category = ConsoleMenu.readLine(scanner, "Category: ");
        int year = ConsoleMenu.readInt(scanner, "Publish Year: ");
        Book book = new Book(id, title, category, year);
        configureCopies(scanner, book);
        book.setAuthor(ConsoleMenu.readLine(scanner, "Author: "));
        String isbn;
        while (true) {
            isbn = ConsoleMenu.readLine(scanner, "ISBN-13 (or 0 to skip): ");
            if ("0".equals(isbn) || Validator.isValidISBN13(isbn)) {
                break;
            }
            ConsoleColor.printError("Invalid ISBN-13. Must be 13 digits starting with 978 or 979.");
        }
        book.setIsbn(isbn);
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