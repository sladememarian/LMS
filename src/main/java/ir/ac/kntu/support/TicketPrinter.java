package ir.ac.kntu.support;

import java.util.List;
import java.util.stream.Collectors;

import ir.ac.kntu.util.ConsoleMenu;

public class TicketPrinter {
    // printing tickets to the console, saving trees
    public static void printTickets(List<SupportTicket> tickets) {
        ConsoleMenu.printAll(tickets);
    }

    public static List<SupportTicket> byCreator(String userId) {
        return SupportService.getAllTickets().stream()
                .filter(t -> t.getUserId() != null
                        && t.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public static List<SupportTicket> byCategoryContains(
            String keyword) {
        String lower = keyword.toLowerCase();
        return SupportService.getAllTickets().stream()
                .filter(t -> t.getSection() != null
                        && t.getSection().name().toLowerCase()
                                .contains(lower))
                .collect(Collectors.toList());
    }
}