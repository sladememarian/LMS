package ir.ac.kntu.finance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.report.ReportService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.EnvConfig;

/**
 * Admin Finance dashboard: read-only oversight of wallets, debts, transactions,
 * tax revenue and reports. Admin inspects but does not hand-edit balances.
 */
public class FinanceAdminConsole {

    private static final String DB_PATH = "finance_secure.json";
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
        ConsoleMenu.option("7", "View Encrypted Database");
        ConsoleMenu.option("8", "Debug Tools");
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
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void doSearchWallet(Scanner scanner) {
        String email = ConsoleMenu.readLine(scanner, "User email: ");
        Persona profile = PersonaService.getProfile(email);
        if (profile == null) {
            ConsoleColor.printError("No user found for " + email + ".");
            return;
        }
        System.out.println("  " + profile.getMemberId() + " | Wallet: " + profile.getWalletBalance()
                + " | Debt: " + FinanceService.getOutstandingDebt(profile.getMemberId()));
    }

    private static void printAllDebts() {
        List<Transaction> all = FinanceService.getAllTransactions();
        for (Transaction tx : all) {
            if ("DEBT".equals(tx.getType()) || "DEBT_PAYMENT".equals(tx.getType())) {
                System.out.println("  " + tx.getMemberId() + " | " + tx.getType() + " " + tx.getAmount());
            }
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
        } catch (IllegalStateException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void inspectDatabase() {
        File file = new File(DB_PATH);
        System.out.println(ConsoleColor.BOLD + "--- " + DB_PATH + " ---" + ConsoleColor.RESET);
        if (!file.exists()) {
            System.out.println(ConsoleColor.gray("  (file does not exist)"));
            return;
        }
        System.out.println("  Size: " + file.length() + " bytes");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] encryptedData = fis.readAllBytes();
            byte[] keyBytes = EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
            byte[] decrypted = new byte[encryptedData.length];
            for (int i = 0; i < encryptedData.length; i++) {
                decrypted[i] = (byte) (encryptedData[i] ^ keyBytes[i % keyBytes.length]);
            }
            System.out.println(new String(decrypted));
        } catch (IOException ex) {
            ConsoleColor.printError("Failed to read: " + ex.getMessage());
        }
    }
}
