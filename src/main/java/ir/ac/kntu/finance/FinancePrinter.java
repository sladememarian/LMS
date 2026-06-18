package ir.ac.kntu.finance;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

/**
 * Shared rendering and the simulated card-payment capture for the Finance
 * dashboards. No real banking happens; the card is validated and the charge is
 * delegated to FinanceService.proccessWalletCharge.
 */
public class FinancePrinter {

    public static void printHeader(Persona user) {
        ConsoleMenu.banner("FINANCE DASHBOARD (" + user.getRole().name() + ")");
        System.out.println(ConsoleColor.BOLD + "Wallet: " + user.getWalletBalance() + ConsoleColor.RESET);
        System.out.println("Current Debt: " + FinanceService.getOutstandingDebt(user.getMemberId()));
    }

    public static void printTransactions(String memberId) {
        List<Transaction> history = FinanceService.getTransactionsForMember(memberId);
        printList(history);
    }

    public static void printList(List<Transaction> history) {
        if (history.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no transactions)"));
            return;
        }
        for (Transaction tx : history) {
            System.out.println("  " + tx.getType() + " " + tx.getAmount()
                    + ConsoleColor.gray("  " + tx.getDescription() + " [" + tx.getTransactionId() + "]"));
        }
    }

    public static void chargeWallet(Scanner scanner, Persona user) {
        System.out.println(ConsoleColor.CYAN + "Charge Wallet (simulated)" + ConsoleColor.RESET);
        String card = ConsoleMenu.readLine(scanner, "Card Number (6037-xxxx-xxxx-xxxx): ");
        ConsoleMenu.readLine(scanner, "Card Holder: ");
        String cvv = ConsoleMenu.readLine(scanner, "CVV: ");
        ConsoleMenu.readLine(scanner, "Expiry (MM/YY): ");
        int amount = ConsoleMenu.readInt(scanner, "Amount: ");
        if (!validCard(card, cvv) || amount <= 0) {
            ConsoleColor.printError("Card details invalid or amount not positive.");
            return;
        }
        FinanceService.proccessWalletCharge(user, amount);
        ConsoleColor.printSuccess("Payment Successful. Wallet updated to " + user.getWalletBalance() + ".");
    }

    private static boolean validCard(String card, String cvv) {
        String digits = card == null ? "" : card.replace("-", "");
        return digits.length() >= 12 && digits.matches("\\d+") && cvv != null && cvv.length() == 3;
    }
}
