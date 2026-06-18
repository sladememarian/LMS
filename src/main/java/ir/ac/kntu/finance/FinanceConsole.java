package ir.ac.kntu.finance;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;

/**
 * Entry point for the Finance microservice UI. Users never start here; Finance
 * is reached from Library/Persona when money is involved. Routes by role.
 */
public class FinanceConsole {

    public static void open(Scanner scanner) {
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            ConsoleColor.printError("Log in first to open Finance.");
            return;
        }
        switch (user.getRole()) {
            case ADMIN:
                FinanceAdminConsole.open(scanner);
                break;
            case CALLCENTER:
                FinanceOperatorConsole.open(scanner);
                break;
            default:
                FinanceMemberConsole.open(scanner, user);
                break;
        }
    }
}
