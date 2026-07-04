package ir.ac.kntu.library;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.persona.InventoryConsole;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserProfile;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class LibraryMemberConsole {
    // member console: where dreams of borrowing come true
    private static final String PROMPT_ITEM_ID = "Item ID: ";
    private static final String NOT_OWNED = "You have not borrowed that item.";
    private static final int EXTENSION_FEE = 25_000;

    public static void open(Scanner scanner, Persona user) {
        boolean active = true;
        while (active) {
            printMenu(user.getUserProfile());
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, user);
        }
    }

    private static void printMenu(UserProfile profile) {
        ConsoleMenu.banner("LIBRARY DASHBOARD (" + profile.dashboardLabel() + ")");
        ConsoleMenu.option("1", "Search / Browse Items");
        ConsoleMenu.option("2", "Filter Items");
        ConsoleMenu.option("3", "View Item Details");
        ConsoleMenu.option("4", "Borrow Item");
        ConsoleMenu.option("5", "Return Item");
        if (profile.canExtend()) {
            ConsoleMenu.option("6", "Extend Return Date");
        }
        ConsoleMenu.option("7", "My Inventory");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona user) {
        switch (choice) {
            case "1":
                doSearch(scanner);
                return true;
            case "2":
                doFilter(scanner);
                return true;
            case "3":
                doDetails(scanner);
                return true;
            case "4":
                doBorrow(scanner, user);
                return true;
            case "5":
                doReturn(scanner, user);
                return true;
            case "6":
                doExtend(scanner, user);
                return true;
            case "7":
                InventoryConsole.show(scanner, user);
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void doSearch(Scanner scanner) {
        String keyword = ConsoleMenu.readLine(scanner, "Search (blank = all): ");
        List<LibraryItem> results = keyword.isEmpty()
                ? LibraryService.getAllItems() : LibraryService.searchItems(keyword);
        LibraryPrinter.printListPaginated(results, false, scanner);
    }

    private static void doFilter(Scanner scanner) {
        String category = ConsoleMenu.readLine(scanner, "Filter by category keyword: ");
        List<LibraryItem> matches = LibraryService.searchItems(category);
        LibraryPrinter.printListPaginated(matches, false, scanner);
    }

    private static void doDetails(Scanner scanner) {
        LibraryItem item = LibraryService.getItemById(ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID));
        if (item == null) {
            ConsoleColor.printError("Item not found.");
            return;
        }
        LibraryPrinter.printDetails(item, false, false);
    }

    private static void doBorrow(Scanner scanner, Persona user) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        if (!canBorrow(user)) {
            return;
        }
        if (user.hasBorrowed(itemId)) {
            ConsoleColor.printError("You already have this item.");
            return;
        }
        if (LibraryService.executeBorrow(itemId)) {
            PersonaService.recordBorrow(user.getEmail(), itemId);
            LoanService.recordLoan(user.getMemberId(), itemId, SimulationClock.getCurrentDay());
            ConsoleColor.printSuccess("Borrowed " + itemId + ".");
        } else {
            ConsoleColor.printError("Borrow failed (item missing or no copies).");
        }
    }

    private static boolean canBorrow(Persona user) {
        UserProfile profile = user.getUserProfile();
        if (!profile.canBorrow()) {
            ConsoleColor.printError("Your role (" + profile.dashboardLabel() + ") cannot borrow items.");
            return false;
        }
        if (user.getBorrowCount() >= profile.borrowLimit()) {
            ConsoleColor.printError("Borrow limit reached for role " + profile.dashboardLabel() + ".");
            return false;
        }
        if (!FinanceService.checkBorrowingPermission(user.getMemberId())) {
            ConsoleColor.printError("Outstanding debt blocks borrowing. Settle it under Finance.");
            return false;
        }
        return true;
    }

    private static void doReturn(Scanner scanner, Persona user) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        if (!user.hasBorrowed(itemId)) {
            ConsoleColor.printError(NOT_OWNED);
            return;
        }
        LibraryService.executeReturn(itemId);
        PersonaService.recordReturn(user.getEmail(), itemId);
        LoanService.clearLoan(user.getMemberId(), itemId);
        ConsoleColor.printSuccess("Returned " + itemId + ".");
    }

    private static void doExtend(Scanner scanner, Persona user) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        if (!user.hasBorrowed(itemId)) {
            ConsoleColor.printError(NOT_OWNED);
            return;
        }
        if (FinanceService.proccessExtentionPayment(user, EXTENSION_FEE)) {
            if (LoanService.extendLoan(user.getMemberId(), itemId, 3)) {
                ConsoleColor.printSuccess("Return date extended +3 days (fee " + EXTENSION_FEE + " charged).");
            } else {
                ConsoleColor.printError("Loan record not found for this item.");
            }
        } else {
            ConsoleColor.printError("Extension failed. Charge your wallet under Finance first.");
        }
    }
}