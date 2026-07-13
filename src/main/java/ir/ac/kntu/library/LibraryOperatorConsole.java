package ir.ac.kntu.library;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class LibraryOperatorConsole {
    // callcenter: the middleman nobody asked for
    private static final String PROMPT_ITEM_ID = "Item ID: ";

    public static void open(Scanner scanner) {
        boolean active = true;
        while (active) {
            printMenu();
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner);
        }
    }

    private static void printMenu() {
        ConsoleMenu.banner("CALLCENTER LIBRARY OPERATOR");
        ConsoleMenu.option("1", "Search Items");
        ConsoleMenu.option("2", "Add Item (via Support)");
        ConsoleMenu.option("3", "Edit Item Price");
        ConsoleMenu.option("4", "Update Quantities (via Support)");
        ConsoleMenu.option("5", "View Supplier Information");
        ConsoleMenu.option("6", "View Item Statuses");
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
                doUpdateQuantity(scanner);
                return true;
            case "5":
                printSuppliers();
                return true;
            case "6":
                LibraryPrinter.printListPaginated(LibraryService.getAllItems(), true, scanner);
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void doAdd(Scanner scanner) {
        Book book = ItemEntry.readNewBook(scanner);
        try {
            SupportService.addLibraryItemViaSupport(book);
            ConsoleColor.printSuccess("Item added: " + book.getItemId());
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doEditPrice(Scanner scanner) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        int price = ConsoleMenu.readInt(scanner, "New Unit Price: ");
        try {
            LibraryService.updateItemPrice(itemId, price);
            ConsoleColor.printSuccess("Price updated.");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doUpdateQuantity(Scanner scanner) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        int delta = ConsoleMenu.readInt(scanner, "Add how many copies: ");
        try {
            SupportService.handleCallCenterStockUpdate(itemId, delta);
            ConsoleColor.printSuccess("Quantities updated for " + itemId + ".");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void printSuppliers() {
        List<SupplierCompany> suppliers = LibraryService.getAllSuppliers();
        for (SupplierCompany supplier : suppliers) {
            System.out.println(ConsoleColor.CYAN + "  " + supplier.getCompanyId() + ConsoleColor.RESET
                    + " - " + supplier.getCompanyName());
        }
    }
}