package ir.ac.kntu.support.inbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.ItemEntry;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.TicketPrinter;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class CallCenterInbox {
    // callcenter: first line of defense against bugs
    private static final String PROMPT_TICKET = "Ticket ID: ";

    public static void open(Scanner scanner, Persona operator) {
        boolean active = true;
        while (active) {
            List<SupportSection> assignedSections = getSortedSections(operator);
            printMenu(assignedSections);
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, operator, assignedSections);
        }
    }

    private static List<SupportSection> getSortedSections(Persona operator) {
        Set<SupportSection> assigned = operator.getAssignedSupportSections();
        List<SupportSection> sorted = new ArrayList<>(assigned);
        sorted.sort(java.util.Comparator.comparing(SupportSection::name));
        return sorted;
    }

    private static void printMenu(List<SupportSection> sections) {
        ConsoleMenu.banner("CALLCENTER INBOX");
        int optionNum = 1;
        for (SupportSection section : sections) {
            ConsoleMenu.option(String.valueOf(optionNum), sectionDisplayName(section) + " Tickets");
            optionNum++;
        }
        int respondNum = optionNum;
        ConsoleMenu.option(String.valueOf(respondNum), "Respond To Ticket");
        ConsoleMenu.option(String.valueOf(respondNum + 1), "Close Ticket");
        ConsoleMenu.option(String.valueOf(respondNum + 2), "Add Library Item");
        ConsoleMenu.option(String.valueOf(respondNum + 3), "View Notifications");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona operator,
            List<SupportSection> sections) {
        int choiceInt = parseChoice(choice);
        if (choiceInt < 0) {
            return true;
        }
        if (choiceInt == 0) {
            return false;
        }
        int sectionCount = sections.size();
        if (choiceInt <= sectionCount) {
            showSectionTickets(operator, sections.get(choiceInt - 1));
            return true;
        }
        return dispatchAction(choiceInt, sectionCount, scanner, operator);
    }

    private static int parseChoice(String choice) {
        try {
            return Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            ConsoleColor.printError("Invalid entry.");
            return -1;
        }
    }

    private static void showSectionTickets(Persona operator, SupportSection section) {
        List<SupportTicket> filtered = filterBySection(
                SupportService.getTicketsForAgent(operator), section);
        TicketPrinter.printTickets(filtered);
    }

    private static boolean dispatchAction(int choiceInt, int sectionCount,
            Scanner scanner, Persona operator) {
        int respondNum = sectionCount + 1;
        int closeNum = sectionCount + 2;
        int addItemNum = sectionCount + 3;
        int notifNum = sectionCount + 4;

        if (choiceInt == respondNum) {
            respondTicket(scanner);
            return true;
        } else if (choiceInt == closeNum) {
            changeStatus(scanner, "CLOSED", "Ticket closed.");
            return true;
        } else if (choiceInt == addItemNum) {
            doAddItem(scanner);
            return true;
        } else if (choiceInt == notifNum) {
            NotificationService.showNotifications(scanner, operator);
            return true;
        }
        ConsoleColor.printError("Invalid entry.");
        return true;
    }

    private static List<SupportTicket> filterBySection(List<SupportTicket> tickets,
            SupportSection section) {
        List<SupportTicket> result = new ArrayList<>();
        for (SupportTicket ticket : tickets) {
            if (ticket.getSection() == section) {
                result.add(ticket);
            }
        }
        return result;
    }

    private static String sectionDisplayName(SupportSection section) {
        switch (section) {
            case BOOK_REQUEST: return "Book Request";
            case TECHNICAL: return "Technical";
            case FINANCE: return "Finance";
            case RESERVATION: return "Reservation";
            default: return section.name();
        }
    }

    private static void respondTicket(Scanner scanner) {
        String ticketId = ConsoleMenu.readLine(scanner, PROMPT_TICKET);
        String message = ConsoleMenu.readLine(scanner, "Response message: ");
        if (message.isEmpty()) {
            ConsoleColor.printError("Response message cannot be empty.");
            return;
        }
        try {
            SupportService.respondToTicket(ticketId, message);
            ConsoleColor.printSuccess("Response sent to the member; ticket marked IN_PROGRESS.");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void changeStatus(Scanner scanner, String status, String okMessage) {
        String ticketId = ConsoleMenu.readLine(scanner, PROMPT_TICKET);
        try {
            SupportService.updateTicketStatus(ticketId, status);
            ConsoleColor.printSuccess(okMessage);
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void doAddItem(Scanner scanner) {
        LibraryItem item = ItemEntry.readNewItem(scanner);
        try {
            SupportService.addLibraryItemViaSupport(item);
            ConsoleColor.printSuccess("Item added via Support: "
                    + item.getItemId());
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }
}
