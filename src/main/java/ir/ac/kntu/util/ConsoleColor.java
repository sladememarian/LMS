package ir.ac.kntu.util;

public class ConsoleColor {
    // making the terminal look fancy since monochrome was boring
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String GRAY = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_PURPLE = "\u001B[95m";

    public static String gray(String text) {
        return GRAY + text + RESET;
    }

    public static String brightRed(String text) {
        return BRIGHT_RED + text + RESET;
    }

    public static String brightGreen(String text) {
        return BRIGHT_GREEN + text + RESET;
    }
    
    public static void printError(String message) {
        System.out.println(RED + BOLD + "\n[ERROR] " + message + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + BOLD + "\n[SUCCESS] " + message + RESET);
    }
}