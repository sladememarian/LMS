package ir.ac.kntu.support;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.util.ConsoleColor;

/**
 * Shared rendering and filtering for support tickets, reused by the member
 * "My Tickets" view and by the CallCenter inbox queues.
 */
public class TicketPrinter {

    public static void printTickets(List<SupportTicket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no tickets)"));
            return;
        }
        for (SupportTicket ticket : tickets) {
            System.out.println(ConsoleColor.CYAN + "  " + ticket.getTicketId() + ConsoleColor.RESET
                    + " | " + ticket.getTitle()
                    + ConsoleColor.gray(" [" + ticket.getCategory() + "/" + ticket.getPriority()
                            + "] " + ticket.getStatus()));
            if (ticket.getResponse() != null && !ticket.getResponse().isEmpty()) {
                System.out.println(ConsoleColor.gray("      Support reply: " + ticket.getResponse()));
            }
        }
    }

    public static List<SupportTicket> byCreator(String userId) {
        List<SupportTicket> result = new ArrayList<>();
        for (SupportTicket ticket : SupportService.getAllTickets()) {
            if (ticket.getUserId() != null && ticket.getUserId().equals(userId)) {
                result.add(ticket);
            }
        }
        return result;
    }

    public static List<SupportTicket> byCategoryContains(String keyword) {
        List<SupportTicket> result = new ArrayList<>();
        for (SupportTicket ticket : SupportService.getAllTickets()) {
            String category = ticket.getCategory() == null ? "" : ticket.getCategory().toLowerCase();
            if (category.contains(keyword.toLowerCase())) {
                result.add(ticket);
            }
        }
        return result;
    }
}
