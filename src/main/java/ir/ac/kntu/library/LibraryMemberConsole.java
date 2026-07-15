package ir.ac.kntu.library;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.persona.InventoryConsole;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserProfile;
import ir.ac.kntu.reservation.Reservation;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.SystemSettingsService;

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
        if (profile.canReserve()) {
            ConsoleMenu.option("8", "Reserve Item");
            ConsoleMenu.option("9", "Cancel Reservation");
            ConsoleMenu.option("10", "My Reservations");
        }
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
                return handleReservation(choice, scanner, user);
        }
    }

    private static boolean handleReservation(String choice, Scanner scanner,
            Persona user) {
        switch (choice) {
            case "8":
                doReserve(scanner, user);
                return true;
            case "9":
                doCancelReservation(scanner, user);
                return true;
            case "10":
                doMyReservations(user);
                return true;
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
        try {
            LibraryItem item = LibraryService.getItemById(itemId);
            if (item == null) {
                ConsoleColor.printError("Item not found.");
                return;
            }
            int walkInAvailable = ReservationService.getWalkInAvailableCopies(
                    itemId, user.getMemberId(),
                    item.getAvailableCopies());
            if (walkInAvailable <= 0) {
                List<String> holders =
                        ReservationService.getActiveReservationHolders(
                                itemId);
                if (holders.isEmpty()) {
                    ConsoleColor.printError(
                            "All copies are currently checked out.");
                } else {
                    ConsoleColor.printError(
                            "This copy is being held for "
                            + holders.size()
                            + " reservation(s).");
                }
                return;
            }
            LibraryService.executeBorrow(itemId);
            PersonaService.recordBorrow(user.getEmail(), itemId);
            int loanPeriodDays = Math.min(                    item.borrowPeriod(),
                    SystemSettingsService.getBorrowDays());
            LoanService.recordLoan(user.getMemberId(), itemId,
                    SimulationClock.getCurrentDay(), loanPeriodDays);
            ReservationService.completeReservation(
                    user.getMemberId(), itemId);
            ConsoleColor.printSuccess("Borrowed " + itemId + ".");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
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
        try {
            LibraryService.executeReturn(itemId);
            PersonaService.recordReturn(user.getEmail(), itemId);
            LoanService.clearLoan(user.getMemberId(), itemId);
            ReservationService.processReturn(itemId,
                    SimulationClock.getCurrentDay());
            ConsoleColor.printSuccess("Returned " + itemId + ".");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doExtend(Scanner scanner, Persona user) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        if (!user.hasBorrowed(itemId)) {
            ConsoleColor.printError(NOT_OWNED);
            return;
        }
        try {
            FinanceService.proccessExtentionPayment(user, EXTENSION_FEE);
            if (LoanService.extendLoan(user.getMemberId(), itemId, 3)) {
                ConsoleColor.printSuccess("Return date extended +3 days (fee " + EXTENSION_FEE + " charged).");
            } else {
                ConsoleColor.printError("Loan record not found for this item.");
            }
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doReserve(Scanner scanner, Persona user) {
        String itemId = ConsoleMenu.readLine(scanner, PROMPT_ITEM_ID);
        try {
            Reservation reservation = ReservationService.reserve(
                    user.getMemberId(), itemId,
                    SimulationClock.getCurrentDay());
            if (reservation.isPending()) {
                int pos = ReservationService.getQueuePosition(
                        reservation.getReservationId(), itemId);
                int holders = ReservationService
                        .getActiveReservationHolders(itemId).size();
                ConsoleColor.printSuccess(
                        "Item unavailable. You are #"
                        + pos + " in queue"
                        + " (" + holders
                        + " active reservation(s) ahead)."
                        + " Pickup window starts when"
                        + " a copy becomes available.");
            } else {
                ConsoleColor.printSuccess(
                        "Reservation activated. Pick up by day "
                        + reservation.getExpiresOnDay() + ".");
            }
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doCancelReservation(Scanner scanner,
            Persona user) {
        List<Reservation> reservations =
                ReservationService.getMemberReservations(
                        user.getMemberId());
        if (reservations.isEmpty()) {
            ConsoleColor.printError("No active reservations to cancel.");
            return;
        }
        for (Reservation reservation : reservations) {
            System.out.println("  " + reservation.getReservationId()
                    + " | Item: " + reservation.getItemId()
                    + " | Status: "
                    + reservation.getStatus().getLabel());
        }
        String resId = ConsoleMenu.readLine(scanner,
                "Enter reservation ID (e.g. RES-XXXXXXXX): ");
        try {
            ReservationService.cancel(resId);
            ConsoleColor.printSuccess("Reservation cancelled.");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doMyReservations(Persona user) {
        List<Reservation> reservations =
                ReservationService.getMemberReservations(
                        user.getMemberId());
        if (reservations.isEmpty()) {
            System.out.println(ConsoleColor.gray(
                    "  No active reservations."));
            return;
        }
        System.out.println("  Active reservations:");
        for (Reservation reservation : reservations) {
            String line = "  " + reservation.getReservationId()
                    + " | Item: " + reservation.getItemId()
                    + " | Status: "
                    + reservation.getStatus().getLabel();
            if (reservation.isPending()) {
                int pos = ReservationService.getQueuePosition(
                        reservation.getReservationId(),
                        reservation.getItemId());
                int ahead = ReservationService
                        .getActiveReservationHolders(
                                reservation.getItemId()).size();
                line += " | Waiting (no deadline)"
                        + " | Queue #" + pos
                        + " (" + ahead + " ahead)";
            } else {
                line += " | Expires: day "
                        + reservation.getExpiresOnDay()
                        + " | Ready for pickup";
            }
            System.out.println(line);
        }
    }
}