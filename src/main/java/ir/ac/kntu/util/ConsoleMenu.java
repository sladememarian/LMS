package ir.ac.kntu.util;

import java.util.Scanner;

/**
 * Small shared helper for the interactive dashboards.
 * Centralises prompt reading, banners and the "press enter" pause so the
 * per-microservice consoles do not duplicate the same console plumbing.
 */
public class ConsoleMenu {

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
}
