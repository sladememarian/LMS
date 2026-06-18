package ir.ac.kntu.library;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;

/**
 * Welcome to library app :) have fun
 * (Admin, CallCenter, student, teacher, guest i guess).
 */
public class LibraryConsole {

    public static void open(Scanner scanner) {
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            ConsoleColor.printError("Log in first to open the Library.");
            return;
        }
        switch (user.getRole()) {
            case ADMIN:
                LibraryAdminConsole.open(scanner);
                break;
            case CALLCENTER:
                LibraryOperatorConsole.open(scanner);
                break;
            default:
                LibraryMemberConsole.open(scanner, user);
                break;
        }
    }
}
