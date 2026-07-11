package ir.ac.kntu.support;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.util.ConsoleMenu;

public class TicketPrinter {
    // printing tickets to the console, saving trees
    public static void printTickets(List<SupportTicket> tickets) {
        ConsoleMenu.printAll(tickets);
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
            String section = ticket.getSection() == null ? "" : ticket.getSection().name();
            if (section.toLowerCase().contains(keyword.toLowerCase())) {
                result.add(ticket);
            }
        }
        return result;
    }
}