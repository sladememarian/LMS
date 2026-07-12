package ir.ac.kntu.library;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;

public class LibraryConsole {

    public static void open(Scanner scanner) {
        // routing users based on their role, like a bouncer
        LibraryService.initCatalog();
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            ConsoleColor.printError("Log in first to open the Library.");
            return;
        }
        user.getUserProfile().openLibraryConsole(scanner, user);
    }
}