package ir.ac.kntu.support;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;

public class SupportConsole {

    public static void open(Scanner scanner) {
        // routing users to the right level of support (or chaos)
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            ConsoleColor.printError("Log in first to open Support.");
            return;
        }
        user.getUserProfile().openSupportConsole(scanner, user);
    }
}