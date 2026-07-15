package ir.ac.kntu.finance;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.generic.Menu;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.report.ReportService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.TransactionRepository;

public class FinanceAdminConsole {
    private static final String REPORT_PATH = "library_financial_report.html";

    public static void open(Scanner scanner) {
        boolean active = true;
        while (active) {
            printMenu();
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner);
        }
    }

    private static void printMenu() {
        ConsoleMenu.banner("ADMIN FINANCE DASHBOARD");
        ConsoleMenu.option("1", "Search User Wallets");
        ConsoleMenu.option("2", "View All Debts");
        ConsoleMenu.option("3", "View Transaction History");
        ConsoleMenu.option("4", "View Tax Revenue");
        ConsoleMenu.option("5", "View Admin Wallet");
        ConsoleMenu.option("6", "View Financial Reports");
        ConsoleMenu.option("7", "View Database Records");
        ConsoleMenu.option("8", "Debug Tools");
        ConsoleMenu.option("9", "View Overdue Loans Report");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner) {
        switch (choice) {
            case "1":
                doSearchWallet(scanner);
                return true;
            case "2":
                printAllDebts();
                return true;
            case "3":
                FinancePrinter.printList(FinanceService.getAllTransactions());
                return true;
            case "4":
                System.out.println("  Tax revenue collected: " + FinanceService.getTaxRevenueCollected());
                return true;
            case "0":
                return false;
            default:
                return handleAdvanced(choice);
        }
    }

    private static boolean handleAdvanced(String choice) {
        switch (choice) {
            case "5":
                printAdminWallet();
                return true;
            case "6":
                doReport();
                return true;
            case "7":
                inspectDatabase();
                return true;
            case "8":
                System.out.println("  Total transactions: " + FinanceService.getAllTransactions().size());
                return true;
            case "9":
                printOverdueReport();
                return true;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void printOverdueReport() {
        new Menu<>("Overdue Loans (as of day " + SimulationClock.getCurrentDay() + ")",
                ReportService.computeOverdueLoans()).render();
    }

    private static void doSearchWallet(Scanner scanner) {
        String email = ConsoleMenu.readLine(scanner, "User email: ");
        Persona profile = PersonaService.getProfile(email);
        if (profile == null) {
            ConsoleColor.printError("No user found for " + email + ".");
            return;
        }
        System.out.println("  " + profile.getEmail() + " | Wallet: " + profile.getWalletBalance()
                + " | Debt: " + FinanceService.getOutstandingDebt(profile.getMemberId()));
    }

    private static void printAllDebts() {
        for (Transaction tx : FinanceService.getDebtAndPaymentTransactions()) {
            Persona owner = PersonaService.getProfileByMemberId(
                    tx.getMemberId());
            String who = owner != null ? owner.getEmail()
                    : tx.getMemberId();
            System.out.println("  " + who + " | " + tx.getType()
                    + " " + tx.getAmount());
        }
    }

    private static void printAdminWallet() {
        Persona admin = PersonaService.getProfileByUsername("admin");
        int balance = admin == null ? 0 : admin.getWalletBalance();
        System.out.println("  Admin wallet (tax pool): " + balance);
    }

    private static void doReport() {
        try {
            String path = ReportService.exportReport(REPORT_PATH);
            ConsoleColor.printSuccess("Report generated at " + path);
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void inspectDatabase() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Transactions", TransactionRepository.getAllTransactions().size());
        ConsoleMenu.printCounts("Finance Database Records", counts);
    }
}
