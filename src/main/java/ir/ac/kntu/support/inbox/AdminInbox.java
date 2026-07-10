package ir.ac.kntu.support.inbox;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.TicketPrinter;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.support.rolerequest.RoleRequest;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.DatabaseAccess;

public class AdminInbox {
    private static final String PROMPT_REQUEST = "Request ID: ";

    public static void open(Scanner scanner, Persona admin) {
        boolean active = true;
        while (active) {
            printMenu();
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, admin);
        }
    }

    private static void printMenu() {
        ConsoleMenu.banner("ADMIN INBOX");
        ConsoleMenu.option("1", "View Role Requests");
        ConsoleMenu.option("2", "Approve Request");
        ConsoleMenu.option("3", "Reject Request");
        ConsoleMenu.option("4", "View User Tickets");
        ConsoleMenu.option("5", "View CallCenter Activity");
        ConsoleMenu.option("6", "View Notifications");
        ConsoleMenu.option("7", "View Database Records");
        ConsoleMenu.option("8", "Debug Tools");
        ConsoleMenu.option("9", "Advance Simulated Day (Time God)");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona admin) {
        switch (choice) {
            case "1":
                printRequests();
                return true;
            case "2":
                review(scanner, true);
                return true;
            case "3":
                review(scanner, false);
                return true;
            case "4":
                TicketPrinter.printTickets(SupportService.getAllTickets());
                return true;
            case "0":
                return false;
            default:
                return handleAdvanced(choice, scanner, admin);
        }
    }

    private static boolean handleAdvanced(String choice, Scanner scanner, Persona admin) {
        switch (choice) {
            case "5":
                printActivity();
                return true;
            case "6":
                NotificationService.showNotifications(scanner, admin);
                return true;
            case "7":
                inspectDatabase();
                return true;
            case "8":
                System.out.println("  Pending role requests: " + RoleRequestService.getPending().size());
                return true;
            case "9":
                advanceSimulatedDay();
                return true;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void advanceSimulatedDay() {
        int newDay = SimulationClock.advanceDay();
        List<String> charges = LoanService.accrueOverdueDebts(newDay);
        ReservationService.expireReservations(newDay);
        System.out.println(ConsoleColor.BOLD + "Simulated day advanced to day " + newDay
                + " (" + SimulationClock.formatCurrentDate() + ")" + ConsoleColor.RESET);
        if (charges.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no overdue fines accrued today)"));
            return;
        }
        System.out.println("  Overdue fines injected into member debts:");
        for (String charge : charges) {
            System.out.println(ConsoleColor.gray("   - " + charge));
        }
    }

    private static void printRequests() {
        List<RoleRequest> pending = RoleRequestService.getPending();
        if (pending.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no pending requests)"));
            return;
        }
        for (RoleRequest request : pending) {
            System.out.println(ConsoleColor.CYAN + "  " + request.getRequestId() + ConsoleColor.RESET
                    + " | " + request.getRequesterEmail() + " -> " + request.getRequestedRole()
                    + ConsoleColor.gray(" : " + request.getMessage()));
        }
    }

    private static void review(Scanner scanner, boolean approve) {
        String requestId = ConsoleMenu.readLine(scanner, PROMPT_REQUEST);
        boolean done = approve
                ? RoleRequestService.approve(requestId) : RoleRequestService.reject(requestId);
        if (done) {
            ConsoleColor.printSuccess("Request " + requestId + (approve ? " approved." : " rejected."));
        } else {
            ConsoleColor.printError("Request not found or already processed.");
        }
    }

    private static void printActivity() {
        int closed = 0;
        for (SupportTicket ticket : SupportService.getAllTickets()) {
            if ("CLOSED".equals(ticket.getStatus())) {
                closed++;
            }
        }
        System.out.println("  Total tickets: " + SupportService.getAllTickets().size());
        System.out.println("  Closed tickets: " + closed);
    }

    private static void inspectDatabase() {
        System.out.println(ConsoleColor.BOLD + "--- Database Records ---" + ConsoleColor.RESET);
        System.out.println("  Personas: " + DatabaseAccess.getAllPersonas().size());
        System.out.println("  Mail messages: " + DatabaseAccess.getAllMailMessages().size());
        System.out.println("  Transactions: " + DatabaseAccess.getAllTransactions().size());
        System.out.println("  Loans: " + DatabaseAccess.getAllLoans().size());
        System.out.println("  Library items: " + DatabaseAccess.getAllLibraryItems().size());
        System.out.println("  Suppliers: " + DatabaseAccess.getAllSuppliers().size());
        System.out.println("  Support tickets: " + DatabaseAccess.getAllSupportTickets().size());
        System.out.println("  Role requests: " + DatabaseAccess.getAllRoleRequests().size());
        System.out.println("  Simulated day: " + SimulationClock.getCurrentDay());
    }
}
