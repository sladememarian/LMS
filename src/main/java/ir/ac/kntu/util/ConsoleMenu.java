package ir.ac.kntu.util;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.interfaces.Displayable;

public class ConsoleMenu {
    // reusable UI components for the terminal aesthetic
    private static final String DIVIDER =
        "=============================================";
    private static final String PAUSE_TEXT = "Press Enter to continue...";

    public static String readLine(Scanner scanner, String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    public static int readInt(Scanner scanner, String label) {
        String raw = readLine(scanner, label);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static long readLong(Scanner scanner, String label) {
        String raw = readLine(scanner, label);
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static void banner(String title) {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + DIVIDER + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "  " + title + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + DIVIDER + ConsoleColor.RESET);
    }

    public static void option(String number, String label) {
        System.out.println(ConsoleColor.CYAN + number + ". " + ConsoleColor.RESET + label);
    }

    public static void back() {
        System.out.println(ConsoleColor.RED + "0. " + ConsoleColor.RESET + "Back");
    }

    public static void pause(Scanner scanner) {
        System.out.println(ConsoleColor.gray(PAUSE_TEXT));
        scanner.nextLine();
    }

    /**
     * Prints any list of Displayable items by calling toDisplayString() on each.
     * This works for SupportTicket, SupplierFinancials, or any future class
     * that implements Displayable -- the menu code never needs to know which.
     */
    public static void printAll(List<? extends Displayable> items) {
        if (items.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (nothing to show)"));
            return;
        }
        for (Displayable item : items) {
            System.out.println(item.toDisplayString());
        }
    }
}