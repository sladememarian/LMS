package ir.ac.kntu.iam;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.Validator;

public class IamService {

    private static final List<UserCredentials> DATABASE = new ArrayList<>();

    private static final String DIVIDER =
        "==============================================";

    private static final String BACK_TO_MENU =
        "Press Enter to head back to Main Menu...";

    private static final String BACK_OPTION = "0";

    public static void signUpMenu(Scanner scanner) {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + DIVIDER + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "            REGISTRATION PORTAL               " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + DIVIDER + ConsoleColor.RESET);

        System.out.print("First Name (or 0 to go back): ");
        String firstName = scanner.nextLine().trim();
        if (BACK_OPTION.equals(firstName)) {
            return;
        }

        System.out.print("Last Name: ");
        String lastName = scanner.nextLine().trim();

        registerLoop(scanner, firstName, lastName);

        System.out.println(ConsoleColor.gray(BACK_TO_MENU));
        scanner.nextLine();
    }

    private static void registerLoop(Scanner scanner, String firstName, String lastName) {
        while (true) {
            try {
                System.out.print("Email (or 0 to cancel): ");
                String email = scanner.nextLine().trim();
                if (BACK_OPTION.equals(email)) {
                    return;
                }

                System.out.print("Phone Number:(+98/98/0) ");
                String phoneNumber = scanner.nextLine().trim();

                System.out.print("Password: ");
                String password = scanner.nextLine().trim();

                registerUser(new UserCredentials(email, password, firstName, lastName, phoneNumber));
                ConsoleColor.printSuccess("Registration successful! A welcome email is in your inbox.");
                break;
            } catch (IllegalArgumentException ex) {
                ConsoleColor.printError(ex.getMessage());
            }
        }
    }

    public static void registerUser(UserCredentials user) {
        DATABASE.add(user);
        String email = user.getEmail();
        PersonaService.registerPersona(email, user.getPassword());
        PersonaService.updateProfile(email, user.getFirstName(), user.getLastName(), user.getPhoneNumber());
        MailService.sendWelcome(email);
    }

    public static void loginMenu(Scanner scanner) {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + DIVIDER + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "                LOGIN PORTAL                  " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + DIVIDER + ConsoleColor.RESET);

        while (true) {
            System.out.print("Email (or 0 to go back): ");
            String email = scanner.nextLine().trim();
            if (BACK_OPTION.equals(email)) {
                return;
            }

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            if (PersonaService.validateCredentials(email, password) && performTwoFactor(scanner, email)) {
                Persona.setCurrentUser(PersonaService.getProfile(email));
                ConsoleColor.printSuccess("Login successful! Welcome to the portal.");
                break;
            }
            ConsoleColor.printError("Login failed. Please try again.");
        }
        System.out.println(ConsoleColor.gray(BACK_TO_MENU));
        scanner.nextLine();
    }

    private static boolean performTwoFactor(Scanner scanner, String email) {
        String code = MailService.deliver2FACode(email);
        System.out.println();
        ConsoleColor.printSuccess("=== MAIL NOTIFICATION ===");
        System.out.println(ConsoleColor.BOLD + "  From: " + MailService.getSystemName() + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "  Subject: Your 2FA Verification Code" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "  Body: Your verification code is " + code
                + ". It expires in " + MailService.getExpireMinutes() + " minutes." + ConsoleColor.RESET);
        ConsoleColor.printSuccess("=========================");
        System.out.println();
        System.out.print("Enter the 2FA code (or 'quit'): ");
        String input = scanner.nextLine().trim();
        if ("quit".equalsIgnoreCase(input)) {
            return false;
        }
        if (MailService.verifyCode(email, input)) {
            return true;
        }
        ConsoleColor.printError("Invalid or expired 2FA code.");
        return false;
    }

    public static boolean changePassword(String email, String currentPassword, String newPassword) {
        if (!PersonaService.validateCredentials(email, currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!Validator.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password does not meet the security policy");
        }
        boolean updated = PersonaService.updatePassword(email, newPassword);
        if (updated) {
            MailService.sendPasswordReset(email);
        }
        return updated;
    }

    public static List<UserCredentials> getRegisteredCredentials() {
        return new ArrayList<>(DATABASE);
    }
}
