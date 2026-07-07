package ir.ac.kntu.library;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.report.ReportService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.DatabaseAccess;

public class LibraryAdminConsole {
    private static final String PROMPT_ITEM_ID = "Item ID: ";
    private static final String REPORT_PATH = "library_financial_report.html";

    public static void open(Scanner scanner) {
        boolean active = true;
        while (active) {
            printMenu();
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner);
        }
    }

    private static void printMenu() {
        ConsoleMenu.banner("ADMIN LIBRARY DASHBOARD");
        ConsoleMenu.option("1", "Search Item");
        ConsoleMenu.option("2", "Add Item");
        ConsoleMenu.option("3", "Edit Item Price");
        ConsoleMenu.option("4", "Delete Item");
        ConsoleMenu.option("5", "Manage Quantities");
        ConsoleMenu.option("6", "View Companies");
        ConsoleMenu.option("7", "Generate HTML Report");
        ConsoleMenu.option("8", "View Borrow Statistics");
        ConsoleMenu.option("9", "View Database Records");
        ConsoleMenu.option("10", "Debug Tools");
        ConsoleMenu.option("11", "View Supplier Financials");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner) {
        switch (choice) {
            case "1":
                LibraryPrinter.printListPaginated(LibraryService.searchItems(
                        ConsoleMenu.readLine(scanner, "Keyword: ")), true, scanner);
                return true;
            case "2":
                doAdd(scanner);
                return true;
            case "3":
                doEditPrice(scanner);
                return true;
            case "4":
                doDelete(scanner);
                return true;
            case "5":
                doManageQuantities(scanner);
                return true;
            case "0":
                return false;
            default:
                return handleAdvanced(choice);
        }
    }

    private static boolean handleAdvanced(String choice) {
        switch (choice) {
            case "6":
                printSuppliers();
                return true;
            case "7":
                doReport();
                return true;
            case "8":
                printBorrowStatistics();
                return true;
            case "9":
                inspectDatabase();
                return true;
            case "10":
                printDebug();
                return true;
            case "11":
                ConsoleMenu.printAll(ReportService.computeSupplierFinancials());
                return true;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void doAdd(Scanner scanner) {
        Book book = ItemEntry.readNewBook(scanner);
        if (LibraryService.addItem(book)) {
            ConsoleColor.printSuccess("Item added: " + book.getItemId());
        } else {
            ConsoleColor.printError("Add rejected (duplicate ID).");
        }
    }

    private static void doEditPrice(Scanner scanner) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        int price = ConsoleMenu.readInt(scanner, "New Unit Price: ");
        if (LibraryService.updateItemPrice(itemId, price)) {
            ConsoleColor.printSuccess("Price updated.");
        } else {
            ConsoleColor.printError("Item not found or invalid price.");
        }
    }

    private static void doDelete(Scanner scanner) {
        if (LibraryService.deleteItem(ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID))) {
            ConsoleColor.printSuccess("Item deleted.");
        } else {
            ConsoleColor.printError("Item not found.");
        }
    }

    private static void doManageQuantities(Scanner scanner) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        int delta = ConsoleMenu.readInt(scanner, "Add how many copies: ");
        LibraryService.updateItemQuantityFromCallCenter(itemId, delta);
    }

    private static void doReport() {
        try {
            String path = ReportService.exportReport(REPORT_PATH);
            ConsoleColor.printSuccess("Report generated at " + path);
        } catch (IllegalStateException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void printSuppliers() {
        for (SupplierCompany supplier : LibraryService.getAllSuppliers()) {
            System.out.println(ConsoleColor.CYAN + "  " + supplier.getCompanyId() + ConsoleColor.RESET
                    + " - " + supplier.getCompanyName());
        }
    }

    private static void printBorrowStatistics() {
        List<LibraryItem> items = LibraryService.getAllItems();
        for (LibraryItem item : items) {
            System.out.println("  " + item.getItemId() + " | " + item.getTitle()
                    + " | borrowed " + item.getBorrowedCopies() + "/" + item.getTotalCopies());
        }
    }

    private static void inspectDatabase() {
        System.out.println(ConsoleColor.BOLD + "--- Library Database Records ---" + ConsoleColor.RESET);
        System.out.println("  Library items: " + DatabaseAccess.getAllLibraryItems().size());
        System.out.println("  Suppliers: " + DatabaseAccess.getAllSuppliers().size());
    }

    private static void printDebug() {
        System.out.println("  Items: " + LibraryService.getAllItems().size());
        System.out.println("  Suppliers: " + LibraryService.getAllSuppliers().size());
    }
}
