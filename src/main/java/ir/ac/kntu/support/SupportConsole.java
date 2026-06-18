package ir.ac.kntu.support;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.support.inbox.AdminInbox;
import ir.ac.kntu.support.inbox.CallCenterInbox;
import ir.ac.kntu.util.ConsoleColor;

/**
 * Entry point for the Support microservice UI. Routes to the Admin inbox, the
 * CallCenter inbox, or the member support dashboard based on the logged-in role.
 */
public class SupportConsole {

    public static void open(Scanner scanner) {
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            ConsoleColor.printError("Log in first to open Support.");
            return;
        }
        switch (user.getRole()) {
            case ADMIN:
                AdminInbox.open(scanner, user);
                break;
            case CALLCENTER:
                CallCenterInbox.open(scanner, user);
                break;
            default:
                SupportMemberConsole.open(scanner, user);
                break;
        }
    }
}
