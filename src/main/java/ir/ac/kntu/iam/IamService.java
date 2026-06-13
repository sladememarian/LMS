package ir.ac.kntu.iam;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.ConsoleColor;

public class IamService {

    private static final List<UserCredentials> DATABASE = new ArrayList<>();

    private static final String DIVIDER = 
        "==============================================";
    
    private static final String BACK_TO_MENU = 
        "Press Enter to head back to Main Menu...";

    public static void signUpMenu(Scanner scanner) {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + DIVIDER + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "            REGISTRATION PORTAL               " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + DIVIDER + ConsoleColor.RESET);

        System.out.print("First Name: ");
        String firstName = scanner.nextLine().trim();

        System.out.print("Last Name: ");
        String lastName = scanner.nextLine().trim();

        while (true) {
            try {
                System.out.print("Email: ");
                String email = scanner.nextLine().trim();

                System.out.print("Phone Number:(+98/98/0) ");
                String phoneNumber = scanner.nextLine().trim();

                System.out.print("Password: ");
                String password = scanner.nextLine().trim();

                UserCredentials newUser = new UserCredentials(email, password, firstName, lastName, phoneNumber);
                DATABASE.add(newUser);
                PersonaService.registerPersona(email, password);
                ConsoleColor.printSuccess("Registration successful! You can now log in.");
                break;
            } catch (IllegalArgumentException e) {
                ConsoleColor.printError(e.getMessage());
            }
        }
        System.out.println(ConsoleColor.gray(BACK_TO_MENU));
        scanner.nextLine();
    }

    public static void loginMenu(Scanner scanner) {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + DIVIDER + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "                LOGIN PORTAL                  " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + DIVIDER + ConsoleColor.RESET);

        while (true) {
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            boolean found = PersonaService.validateCredentials(email, password);
            // for (UserCredentials user : DATABASE) {
            //     if (user.getEmail().equals(email) && user.getPassword().equals(password)) {
            //         ConsoleColor.printSuccess("Login successful! Welcome, " + user.getFirstName() + "!");
            //         found = true;
            //         break;
            //     }
            // }
            if (found) {
                Persona.currentUser = PersonaService.getProfile(email);
                ConsoleColor.printSuccess("Login successful! Welcome to the portal.");
                break;
            } else {
                ConsoleColor.printError("Invalid email or password. Please try again.");
            }
        }
        System.out.println(ConsoleColor.gray(BACK_TO_MENU));
        scanner.nextLine();
    }
}