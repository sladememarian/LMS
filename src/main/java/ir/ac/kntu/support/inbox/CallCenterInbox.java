package ir.ac.kntu.support.inbox;

import java.util.Scanner;

import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.ItemEntry;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.TicketPrinter;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

/**
 * CallCenter inbox: the operator side of Support. Handles ticket queues,
 * responses and the CallCenter -> Support -> Library item-add bridge.
 */
public class CallCenterInbox {

    private static final String PROMPT_TICKET = "Ticket ID: ";

    public static void open(Scanner scanner, Persona operator) {
        boolean active = true;
        while (active) {
            printMenu();
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, operator);
        }
    }

    private static void printMenu() {
        ConsoleMenu.banner("CALLCENTER INBOX");
        ConsoleMenu.option("1", "Technical Tickets");
        ConsoleMenu.option("2", "Book Requests");
        ConsoleMenu.option("3", "Respond To Ticket");
        ConsoleMenu.option("4", "Close Ticket");
        ConsoleMenu.option("5", "Add Library Item");
        ConsoleMenu.option("6", "View Notifications");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona operator) {
        switch (choice) {
            case "1":
                TicketPrinter.printTickets(TicketPrinter.byCategoryContains("Technical"));
                return true;
            case "2":
                TicketPrinter.printTickets(TicketPrinter.byCategoryContains("Book"));
                return true;
            case "3":
                respondTicket(scanner);
                return true;
            case "4":
                changeStatus(scanner, "CLOSED", "Ticket closed.");
                return true;
            case "5":
                doAddItem(scanner);
                return true;
            case "6":
                NotificationService.showNotifications(scanner, operator);
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void respondTicket(Scanner scanner) {
        String ticketId = ConsoleMenu.readLine(scanner, PROMPT_TICKET);
        String message = ConsoleMenu.readLine(scanner, "Response message: ");
        if (message.isEmpty()) {
            ConsoleColor.printError("Response message cannot be empty.");
            return;
        }
        if (SupportService.respondToTicket(ticketId, message)) {
            ConsoleColor.printSuccess("Response sent to the member; ticket marked IN_PROGRESS.");
        } else {
            ConsoleColor.printError("Ticket not found.");
        }
    }

    private static void changeStatus(Scanner scanner, String status, String okMessage) {
        String ticketId = ConsoleMenu.readLine(scanner, PROMPT_TICKET);
        if (SupportService.updateTicketStatus(ticketId, status)) {
            ConsoleColor.printSuccess(okMessage);
        } else {
            ConsoleColor.printError("Ticket not found.");
        }
    }

    private static void doAddItem(Scanner scanner) {
        Book book = ItemEntry.readNewBook(scanner);
        if (SupportService.addLibraryItemViaSupport(book)) {
            ConsoleColor.printSuccess("Item added via Support: " + book.getItemId());
        } else {
            ConsoleColor.printError("Add rejected (duplicate ID or no permission).");
        }
    }
}
