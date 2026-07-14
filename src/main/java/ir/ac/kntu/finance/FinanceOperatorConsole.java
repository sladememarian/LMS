package ir.ac.kntu.finance;

import java.util.Scanner;

import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class FinanceOperatorConsole {
    // view only - because trust but verify

    public static void open(Scanner scanner) {
        boolean active = true;
        while (active) {
            ConsoleMenu.banner("CALLCENTER FINANCE (VIEW ONLY)");
            ConsoleMenu.option("1", "View Debt Statistics");
            ConsoleMenu.option("2", "View Financial Alerts");
            ConsoleMenu.back();
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice);
        }
    }

    private static boolean handle(String choice) {
        switch (choice) {
            case "1":
                printDebtStatistics();
                return true;
            case "2":
                printAlerts();
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void printDebtStatistics() {
        System.out.println("  Outstanding (aggregate): " + FinanceService.getTotalOutstandingDebt());
        System.out.println("  Debt records: " + FinanceService.getOpenDebtTransactions().size());
    }

    private static void printAlerts() {
        for (Transaction tx : FinanceService.getOpenDebtTransactions()) {
            System.out.println(ConsoleColor.brightRed("  ALERT ") + tx.getMemberId()
                    + " owes " + tx.getAmount());
        }
    }
}