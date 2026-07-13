package ir.ac.kntu.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserProfile;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class SupportMemberConsole {
    private static final String CAT_TECHNICAL = "TECHNICAL";
    private static final String CAT_BOOK = "BOOK_REQUEST";
    private static final String CAT_FINANCE = "FINANCE";
    private static final String CAT_RESERVATION = "RESERVATION";
    private static final String GUEST_ONLY = "Only guests can request a role upgrade.";
    private static final String PROMPT_TITLE = "Title: ";
    private static final String PROMPT_MESSAGE = "Message: ";

    public static void open(Scanner scanner, Persona user) {
        boolean active = true;
        while (active) {
            List<String> labels = new ArrayList<>();
            List<Consumer<Scanner>> actions = new ArrayList<>();
            buildMenu(labels, actions, user);
            printMenu(user.getUserProfile(), labels);
            String choice = ConsoleMenu.readLine(scanner,
                    ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = dispatch(choice, actions, scanner);
        }
    }

    private static void buildMenu(List<String> labels,
            List<Consumer<Scanner>> actions, Persona user) {
        UserProfile profile = user.getUserProfile();
        if (profile.canRequestRoleUpgrade()) {
            labels.add("Request Student Role");
            actions.add(s -> requestRole(s, user, UserRole.STUDENT));
            labels.add("Request Teacher Role");
            actions.add(s -> requestRole(s, user, UserRole.TEACHER));
        }
        labels.add("Create Technical Ticket");
        actions.add(s -> createTicket(s, user, CAT_TECHNICAL));
        labels.add("Create Book Request Ticket");
        actions.add(s -> createTicket(s, user, CAT_BOOK));
        labels.add("Create Finance Ticket");
        actions.add(s -> createTicket(s, user, CAT_FINANCE));
        labels.add("Create Reservation Ticket");
        actions.add(s -> createTicket(s, user, CAT_RESERVATION));
        labels.add("View My Tickets");
        actions.add(s -> TicketPrinter.printTickets(
                TicketPrinter.byCreator(user.getMemberId())));
        labels.add("View Notifications");
        actions.add(s -> NotificationService.showNotifications(s, user));
    }

    private static void printMenu(UserProfile profile, List<String> labels) {
        ConsoleMenu.banner("SUPPORT DASHBOARD (" + profile.dashboardLabel() + ")");
        for (int i = 0; i < labels.size(); i++) {
            ConsoleMenu.option(String.valueOf(i + 1), labels.get(i));
        }
        ConsoleMenu.back();
    }

    private static boolean dispatch(String choice,
            List<Consumer<Scanner>> actions, Scanner scanner) {
        if ("0".equals(choice)) {
            return false;
        }
        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < actions.size()) {
                actions.get(index).accept(scanner);
                return true;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        ConsoleColor.printError("Invalid entry.");
        return true;
    }

    private static void requestRole(Scanner scanner, Persona user, UserRole role) {
        if (!user.getUserProfile().canRequestRoleUpgrade()) {
            ConsoleColor.printError(GUEST_ONLY);
            return;
        }
        String message = ConsoleMenu.readLine(scanner, PROMPT_MESSAGE);
        RoleRequestService.submit(user, role.name(), message);
        ConsoleColor.printSuccess("Request for " + role.name()
                + " sent to the Admin inbox.");
    }

    private static void createTicket(Scanner scanner, Persona user,
            String category) {
        String title = ConsoleMenu.readLine(scanner, PROMPT_TITLE);
        String description = ConsoleMenu.readLine(scanner, "Description: ");
        SupportService.createTicket(user.getMemberId(),
                SupportSection.valueOf(category), title, description);
        ConsoleColor.printSuccess("Ticket created.");
    }
}
