package ir.ac.kntu.finance;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;

public class FinanceConsole {

    public static void open(Scanner scanner) {
        // money talks, this console listens
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