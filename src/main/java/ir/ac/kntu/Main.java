package ir.ac.kntu;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.finance.FinanceConsole;
import ir.ac.kntu.iam.IamService;
import ir.ac.kntu.library.LibraryConsole;
import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.sso.SessionManager;
import ir.ac.kntu.sso.SsoController;
import ir.ac.kntu.support.SupportConsole;
import ir.ac.kntu.util.ConsoleColor;

public class Main {

    public static void main(String[] args) {
        // TODO: convert this to a proper GUI (lol, as if)
        boolean running = true;
        printHeader();
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                printMainMenu();
                System.out.print(ConsoleColor.BOLD + ConsoleColor.YELLOW
                        + "Enter your choice (1-8): " + ConsoleColor.RESET);
                String choice = scanner.nextLine().trim();
                running = route(choice, scanner);
                System.out.println();
            }
        }
    }

    private static boolean route(String choice, Scanner scanner) {
        switch (choice) {
            case "1":
                IamService.signUpMenu(scanner);
                return true;
            case "2":
                handleLogin(scanner);
                return true;
            case "3":
                SsoController.settingsMenu(scanner);
                return true;
            case "4":
                showInbox();
                return true;
            case "5":
                supportEntry(scanner);
                return true;
            case "6":
                LibraryConsole.open(scanner);
                return true;
            case "7":
                FinanceConsole.open(scanner);
                return true;
            case "8":
                System.out.println(ConsoleColor.RED + ConsoleColor.BOLD
                        + "\nShutting down. Goodbye!" + ConsoleColor.RESET);
                return false;
            default:
                System.out.println(ConsoleColor.BRIGHT_RED + "\n[ERROR] Invalid entry." + ConsoleColor.RESET);
                return true;
        }
    }



    private static void handleLogin(Scanner scanner) {
        IamService.loginMenu(scanner);
        Persona user = Persona.getCurrentUser();
        if (user != null) {
            SessionManager.createSession(user);
            SsoController.showSessionInfo();
        }
    }

    private static void showInbox() {
        Persona user = Persona.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            ConsoleColor.printError("Log in first to read your inbox.");
            return;
        }
        Inbox inbox = MailService.getInbox(user.getEmail());
        List<MailMessage> messages = inbox.getMessages();
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD + "INBOX: " + user.getEmail() + ConsoleColor.RESET);
        if (messages.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no messages yet)"));
            return;
        }
        for (MailMessage message : messages) {
            System.out.println(ConsoleColor.gray("  [" + message.getMessageType().getLabel() + "] "
                    + message.getSubject() + " :: " + message.getBody()));
        }
        MailService.markInboxRead(user.getEmail());
    }

    private static void supportEntry(Scanner scanner) {
        Persona current = Persona.getCurrentUser();
        if (current == null) {
            ConsoleColor.printError("Please log in first using option 2 from the main menu.");
            return;
        }
        SupportConsole.open(scanner);
    }

    private static void printHeader() {
        System.out.println(ConsoleColor.BLUE + ConsoleColor.BOLD
                + "====================================================" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD
                + "       WELCOME TO THE MODULAR MONOLITH LMS          " + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BLUE + ConsoleColor.BOLD
                + "====================================================" + ConsoleColor.RESET);
    }

    private static void printMainMenu() {
        System.out.println(ConsoleColor.CYAN + ConsoleColor.BOLD
                + "  [Simulated Date: " + SimulationClock.formatCurrentDate()
                + " | Day " + SimulationClock.getCurrentDay() + "]" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BOLD + "MAIN MENU OPTIONS:" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.CYAN + "1. " + ConsoleColor.RESET + "Sign Up       "
                + ConsoleColor.gray("- Registration, Welcome Mail"));
        System.out.println(ConsoleColor.CYAN + "2. " + ConsoleColor.RESET + "Login         "
                + ConsoleColor.gray("- Credentials + 2FA via Mail"));
        System.out.println(ConsoleColor.CYAN + "3. " + ConsoleColor.RESET + "Settings      "
                + ConsoleColor.gray("- SSO: profile, password, theme"));
        System.out.println(ConsoleColor.CYAN + "4. " + ConsoleColor.RESET + "Mail Inbox    "
                + ConsoleColor.gray("- Read simulated messages"));
        System.out.println(ConsoleColor.CYAN + "5. " + ConsoleColor.RESET + "Support       "
                + ConsoleColor.gray("- Tickets, roles, staff inbox"));
        System.out.println(ConsoleColor.CYAN + "6. " + ConsoleColor.RESET + "Library       "
                + ConsoleColor.gray("- Role-based catalog & borrowing"));
        System.out.println(ConsoleColor.CYAN + "7. " + ConsoleColor.RESET + "Finance       "
                + ConsoleColor.gray("- Wallet, debts, payments"));
        System.out.println(ConsoleColor.RED + "8. Exit Application" + ConsoleColor.RESET);
        System.out.println(ConsoleColor.BLUE
                + "----------------------------------------------------" + ConsoleColor.RESET);
    }
}