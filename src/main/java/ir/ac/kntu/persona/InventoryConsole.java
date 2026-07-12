package ir.ac.kntu.persona;

import java.util.Scanner;

import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryPrinter;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class InventoryConsole {
    // showing users what they've hoarded so far
    public static void show(Scanner scanner, Persona user) {
        ConsoleMenu.banner("MY INVENTORY (" + user.getRole().name() + ")");
        int limit = user.getRole().getMaxBorrowLimit();
        System.out.println(ConsoleColor.BOLD + "Borrowed Items: "
                + user.getBorrowCount() + " / " + limitLabel(limit) + ConsoleColor.RESET);
        if (user.getBorrowCount() == 0) {
            System.out.println(ConsoleColor.gray("  You have not borrowed any items yet."));
            ConsoleMenu.pause(scanner);
            return;
        }
        for (String itemId : user.getBorrowedItemIds()) {
            LibraryItem item = LibraryService.getItemById(itemId);
            if (item == null) {
                System.out.println(ConsoleColor.gray("  " + itemId + " (no longer in catalog)"));
            } else {
                LibraryPrinter.printDetails(item, false, false);
            }
            printDueStatus(user, itemId);
        }
        ConsoleMenu.pause(scanner);
    }

    private static void printDueStatus(Persona user, String itemId) {
        int currentDay = SimulationClock.getCurrentDay();
        int dueDay = LoanService.getDueDay(user.getMemberId(), itemId);
        if (dueDay < 0) {
            return;
        }
        if (LoanService.isOverdue(user.getMemberId(), itemId, currentDay)) {
            int daysLate = currentDay - dueDay;
            System.out.println(ConsoleColor.RED + "    Overdue by " + daysLate
                    + " day(s), was due on day " + dueDay + ConsoleColor.RESET);
        } else {
            System.out.println(ConsoleColor.gray("    Due on day " + dueDay));
        }
    }

    private static String limitLabel(int limit) {
        return limit == Integer.MAX_VALUE ? "unlimited" : String.valueOf(limit);
    }
}