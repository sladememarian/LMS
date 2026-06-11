package ir.ac.kntu.iam;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.Validator;

public class IamService {

    private static final List<UserCredentials> database = new ArrayList<>();

    public static void signUpMenu(Scanner scanner) {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + "==============================================" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "            REGISTRATION PORTAL               " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + "==============================================" + ConsoleColor.RESET);
        try {
            System.out.print("First Name: ");
            String firstName = scanner.nextLine().trim();

            System.out.print("Last Name: ");
            String lastName = scanner.nextLine().trim();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            System.out.print("Phone Number: ");
            String phoneNumber = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            UserCredentials newUser = new UserCredentials(email, password, firstName, lastName, phoneNumber);
            database.add(newUser);
            ConsoleColor.printSuccess("Registration successful! You can now log in.");
        } catch (IllegalArgumentException e) {
            ConsoleColor.printError(e.getMessage());
        }
    }
}
