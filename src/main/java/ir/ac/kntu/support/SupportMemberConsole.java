package ir.ac.kntu.support;

import java.util.Scanner;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserProfile;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class SupportMemberConsole {
    // members typing their problems hoping for solutions
    private static final String CAT_TECHNICAL = "Technical";
    private static final String CAT_BOOK = "BookRequest";
    private static final String CAT_FINANCE = "Finance";
    private static final String CAT_RESERVATION = "Reservation";
    private static final String GUEST_ONLY = "Only guests can request a role upgrade.";
    private static final String PROMPT_TITLE = "Title: ";
    private static final String PROMPT_MESSAGE = "Message: ";

    public static void open(Scanner scanner, Persona user) {
        boolean active = true;
        while (active) {
            printMenu(user.getUserProfile());
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, user);
        }
    }

    private static void printMenu(UserProfile profile) {
        ConsoleMenu.banner("SUPPORT DASHBOARD (" + profile.dashboardLabel() + ")");
        if (profile.canRequestRoleUpgrade()) {
            ConsoleMenu.option("1", "Request Student Role");
            ConsoleMenu.option("2", "Request Teacher Role");
        }
        ConsoleMenu.option("3", "Create Technical Ticket");
        ConsoleMenu.option("4", "Create Book Request Ticket");
        ConsoleMenu.option("5", "Create Finance Ticket");
        ConsoleMenu.option("6", "Create Reservation Ticket");
        ConsoleMenu.option("7", "View My Tickets");
        ConsoleMenu.option("8", "View Notifications");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona user) {
        if (handleRoleOrTicket(choice, scanner, user)) {
            return true;
        }
        switch (choice) {
            case "7":
                TicketPrinter.printTickets(TicketPrinter.byCreator(user.getMemberId()));
                return true;
            case "8":
                NotificationService.showNotifications(scanner, user);
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static boolean handleRoleOrTicket(String choice, Scanner scanner, Persona user) {
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
                createTicket(scanner, user, CAT_FINANCE);
                return true;
            case "6":
                createTicket(scanner, user, CAT_RESERVATION);
                return true;
            default:
                return false;
        }
    }

    private static void requestRole(Scanner scanner, Persona user, UserRole role) {
        if (!user.getUserProfile().canRequestRoleUpgrade()) {
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
        SupportService.createTicket(user.getMemberId(), SupportSection.valueOf(category.toUpperCase()), title, description);
        ConsoleColor.printSuccess("Ticket created.");
    }
}
