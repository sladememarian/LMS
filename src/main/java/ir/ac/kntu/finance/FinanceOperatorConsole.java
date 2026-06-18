package ir.ac.kntu.finance;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

/**
 * Finance dashboard for the CallCenter role. View-only: CallCenter never moves
 * money, it only reads summarised debt information to help users.
 */
public class FinanceOperatorConsole {

    private static final String TYPE_DEBT = "DEBT";

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
        List<Transaction> all = FinanceService.getAllTransactions();
        int outstanding = 0;
        int debtRecords = 0;
        for (Transaction tx : all) {
            if (TYPE_DEBT.equals(tx.getType())) {
                outstanding += tx.getAmount();
                debtRecords++;
            } else if ("DEBT_PAYMENT".equals(tx.getType())) {
                outstanding -= tx.getAmount();
            }
        }
        System.out.println("  Outstanding (aggregate): " + outstanding);
        System.out.println("  Debt records: " + debtRecords);
    }

    private static void printAlerts() {
        for (Transaction tx : FinanceService.getAllTransactions()) {
            if (TYPE_DEBT.equals(tx.getType())) {
                System.out.println(ConsoleColor.brightRed("  ALERT ") + tx.getMemberId()
                        + " owes " + tx.getAmount());
            }
        }
    }
}
