package ir.ac.kntu.support;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.support.inbox.AdminInbox;
import ir.ac.kntu.support.inbox.CallCenterInbox;
import ir.ac.kntu.util.ConsoleColor;

public class SupportConsole {

    public static void open(Scanner scanner) {
        // routing users to the right level of support (or chaos)
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