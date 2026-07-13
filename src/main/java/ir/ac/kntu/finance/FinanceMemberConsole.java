package ir.ac.kntu.finance;

import java.util.Scanner;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserProfile;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class FinanceMemberConsole {
    // members checking their wallet more than their email
    private static final int EXTENSION_FEE = 25_000;

    public static void open(Scanner scanner, Persona user) {
        boolean active = true;
        while (active) {
            FinancePrinter.printHeader(user);
            printMenu(user.getUserProfile());
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, user);
        }
    }

    private static void printMenu(UserProfile profile) {
        ConsoleMenu.option("1", "Charge Wallet");
        ConsoleMenu.option("2", "Pay Debt");
        if (profile.canExtend()) {
            ConsoleMenu.option("3", "Extend Return Date");
        }
        ConsoleMenu.option("4", "View Transaction History");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona user) {
        switch (choice) {
            case "1":
                FinancePrinter.chargeWallet(scanner, user);
                return true;
            case "2":
                doPayDebt(user);
                return true;
            case "3":
                doExtend(user);
                return true;
            case "4":
                FinancePrinter.printTransactions(user.getMemberId());
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void doPayDebt(Persona user) {
        try {
            FinanceService.payDebt(user);
            ConsoleColor.printSuccess("Debt cleared. Wallet now " + user.getWalletBalance() + ".");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doExtend(Persona user) {
        if (!user.getUserProfile().canExtend()) {
            ConsoleColor.printError(user.getUserProfile().dashboardLabel() + " cannot extend return dates.");
            return;
        }
        try {
            FinanceService.proccessExtentionPayment(user, EXTENSION_FEE);
            ConsoleColor.printSuccess("Return date extended (fee " + EXTENSION_FEE + ").");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }
}