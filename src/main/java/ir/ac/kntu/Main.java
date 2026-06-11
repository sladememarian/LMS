package ir.ac.kntu;

import java.util.Scanner;

import ir.ac.kntu.util.ConsoleColor;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printHeader();

        while (running) {
            printMainMenu();
            
            System.out.print(ConsoleColor.BOLD + ConsoleColor.YELLOW + "Enter your choice (1-8): " + ConsoleColor.RESET);
            
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println(ConsoleColor.GREEN + "\n[IAM] Navigating to Sign Up..." + ConsoleColor.RESET);
                    break;
                case "2":
                    System.out.println(ConsoleColor.GREEN + "\n[IAM] Navigating to Login..." + ConsoleColor.RESET);
                    break;
                case "8":
                    System.out.println(ConsoleColor.RED + ConsoleColor.BOLD + "\nShutting down. Goodbye!" + ConsoleColor.RESET);
                    running = false;
                    break;
                default:
                    System.out.println(ConsoleColor.BRIGHT_RED + "\n[ERROR] Invalid entry." + ConsoleColor.RESET);
                    break;
            }
            System.out.println();
        }
        scanner.close();
    }

    private static void printHeader() {
        System.out.println(ConsoleColor.BLUE + ConsoleColor.BOLD + "====================================================" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + "       WELCOME TO THE MODULAR MONOLITH LMS          " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BLUE + ConsoleColor.BOLD + "====================================================" + ConsoleColor.RESET);
    }

    private static void printMainMenu() {
        System.out.println(ConsoleColor.BOLD + "MAIN MENU OPTIONS:" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + "1. " + ConsoleColor.RESET + "Sign Up       " + ConsoleColor.gray("- Sign In, Registration, 2FA"));
        System.out.println(ConsoleColor.CYAN + "2. " + ConsoleColor.RESET + "Login   " + ConsoleColor.gray("- User Profiles, Borrow Limits"));
        System.out.println(ConsoleColor.RED + "8. Exit Application" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BLUE + "----------------------------------------------------" + ConsoleColor.RESET);
    }
}