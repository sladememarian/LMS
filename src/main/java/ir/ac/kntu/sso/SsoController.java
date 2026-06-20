package ir.ac.kntu.sso;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;

import ir.ac.kntu.util.ConsoleColor;

public class SsoController {

    private static final String DIVIDER =
        "----------------------------------------------";

    public static void settingsMenu(Scanner scanner) {
        Persona user = Persona.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            ConsoleColor.printError("You must be logged in to open Settings.");
            return;
        }
        boolean active = true;
        while (active) {
            printMenu();
            System.out.print(ConsoleColor.BOLD + ConsoleColor.YELLOW + "Choose (0-5): " + ConsoleColor.RESET);
            String choice = scanner.nextLine().trim();
            active = handleChoice(choice, scanner, user.getEmail());
        }
    }

    private static void printMenu() {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + DIVIDER + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "                 SETTINGS (SSO)               " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + DIVIDER + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + "1. " + ConsoleColor.RESET + "View Profile");
        System.out.println(ConsoleColor.CYAN + "2. " + ConsoleColor.RESET + "Edit Profile");
        System.out.println(ConsoleColor.CYAN + "3. " + ConsoleColor.RESET + "Change Password");
        System.out.println(ConsoleColor.CYAN + "4. " + ConsoleColor.RESET + "Theme Settings");
        System.out.println(ConsoleColor.RED + "5. " + ConsoleColor.RESET + "Logout");
        System.out.println(ConsoleColor.RED + "0. " + ConsoleColor.RESET + "Back");
    }

    private static boolean handleChoice(String choice, Scanner scanner, String email) {
        switch (choice) {
            case "1":
                System.out.println(ConsoleColor.gray(SsoService.viewProfile(email)));
                return true;
            case "2":
                doEditProfile(scanner, email);
                return true;
            case "3":
                doChangePassword(scanner, email);
                return true;
            case "4":
                doChangeTheme(scanner, email);
                return true;
            case "0":
                return false;
            case "5":
                SsoService.logout();
                ConsoleColor.printSuccess("Session destroyed. You have been logged out.");
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void doEditProfile(Scanner scanner, String email) {
        System.out.print("New First Name: ");
        String firstName = scanner.nextLine().trim();
        System.out.print("New Last Name: ");
        String lastName = scanner.nextLine().trim();
        System.out.print("New Phone:(+98/98/0) ");
        String phone = scanner.nextLine().trim();
        try {
            SsoService.editProfile(email, firstName, lastName, phone);
            ConsoleColor.printSuccess("Profile updated.");
        } catch (IllegalArgumentException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doChangePassword(Scanner scanner, String email) {
        System.out.print("Current Password: ");
        String current = scanner.nextLine().trim();
        System.out.print("New Password: ");
        String newPass = scanner.nextLine().trim();
        System.out.print("Confirm New Password: ");
        String confirm = scanner.nextLine().trim();
        try {
            SsoService.changePassword(email, current, newPass, confirm);
            ConsoleColor.printSuccess("Password updated. A confirmation email is in your inbox.");
        } catch (IllegalArgumentException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doChangeTheme(Scanner scanner, String email) {
        System.out.print("Theme (LIGHT/DARK): ");
        String theme = scanner.nextLine().trim();
        try {
            SsoService.changeTheme(email, theme);
            ConsoleColor.printSuccess("Theme set to " + SsoService.getTheme(email) + ".");
        } catch (IllegalArgumentException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    public static void showSessionInfo() {
        if (SessionManager.isActive()) {
            System.out.println(ConsoleColor.gray("Active session " + SessionManager.getSessionToken()
                    + " since " + SessionManager.getLoginTime()));
        } else {
            System.out.println(ConsoleColor.gray("No active session."));
        }
    }
}
