package ir.ac.kntu.support;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class SupportMemberConsole {
    // members typing their problems hoping for solutions
    private static final String CAT_TECHNICAL = "Technical";
    private static final String CAT_BOOK = "BookRequest";
    private static final String GUEST_ONLY = "Only guests can request a role upgrade.";
    private static final String PROMPT_TITLE = "Title: ";
    private static final String PROMPT_MESSAGE = "Message: ";

    public static void open(Scanner scanner, Persona user) {
        boolean active = true;
        while (active) {
            printMenu(user.getRole());
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, user);
        }
    }

    private static void printMenu(UserRole role) {
        ConsoleMenu.banner("SUPPORT DASHBOARD (" + role.name() + ")");
        if (role == UserRole.GUEST) {
            ConsoleMenu.option("1", "Request Student Role");
            ConsoleMenu.option("2", "Request Teacher Role");
        }
        ConsoleMenu.option("3", "Create Technical Ticket");
        ConsoleMenu.option("4", "Create Book Request Ticket");
        ConsoleMenu.option("5", "View My Tickets");
        ConsoleMenu.option("6", "View Notifications");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona user) {
        switch (choice) {
            case "1":
                requestRole(scanner, user, UserRole.STUDENT);
                return true;
            case "2":
                requestRole(scanner, user, UserRole.TEACHER);
                return true;
            case "3":
                createTicket(scanner, user, CAT_TECHNICAL);
                return true;
            case "4":
                createTicket(scanner, user, CAT_BOOK);
                return true;
            case "5":
                TicketPrinter.printTickets(TicketPrinter.byCreator(user.getMemberId()));
                return true;
            case "6":
                NotificationService.showNotifications(scanner, user);
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void requestRole(Scanner scanner, Persona user, UserRole role) {
        if (user.getRole() != UserRole.GUEST) {
            ConsoleColor.printError(GUEST_ONLY);
            return;
        }
        String message = ConsoleMenu.readLine(scanner, PROMPT_MESSAGE);
        RoleRequestService.submit(user, role.name(), message);
        ConsoleColor.printSuccess("Request for " + role.name() + " sent to the Admin inbox.");
    }

    private static void createTicket(Scanner scanner, Persona user, String category) {
        String title = ConsoleMenu.readLine(scanner, PROMPT_TITLE);
        String description = ConsoleMenu.readLine(scanner, "Description: ");
        SupportService.createTicket(user.getMemberId(), category, title, description);
        ConsoleColor.printSuccess("Ticket created.");
    }
}