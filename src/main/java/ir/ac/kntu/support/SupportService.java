package ir.ac.kntu.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.DatabaseAccess;

public class SupportService {
    private static final List<SupportTicket> TICKETS = new ArrayList<>();

    static {
        TICKETS.addAll(DatabaseAccess.getAllSupportTickets());
        Collections.sort(TICKETS);
    }

    public static boolean validateCallCenterLogin(String email, String password) {
        if (!PersonaService.validateCredentials(email, password)) {
            return false;
        }
        Persona profile = PersonaService.getProfile(email);
        if (profile != null && profile.getRole() == UserRole.CALLCENTER) {
            Persona.setCurrentUser(profile);
            return true;
        }
        return false;
    }

    public static void createTicket(String userId, SupportSection section, String title, String description) {
        TICKETS.clear();
        TICKETS.addAll(DatabaseAccess.getAllSupportTickets());
        String priority = "LOW";
        if (SupportSection.TECHNICAL == section) {
            priority = "HIGH";
        }
        String upperTitle = title.toUpperCase();
        if (upperTitle.contains("URGENT") || upperTitle.contains("CRASH") || upperTitle.contains("BLOCK")) {
            priority = "CRITICAL";
        }

        String id = "TCK-" + ((int) (Math.random() * 900_000) + 100_000);
        SupportTicket ticket = new SupportTicket(id, userId, title, description, section);
        ticket.setPriority(priority);
        ticket.setStatus("OPEN");
        TICKETS.add(ticket);
        Collections.sort(TICKETS);
        DatabaseAccess.insertSupportTicket(ticket);
    }

    public static boolean updateTicketStatus(String ticketId, String status) {
        TICKETS.clear();
        TICKETS.addAll(DatabaseAccess.getAllSupportTickets());
        for (SupportTicket ticket : TICKETS) {
            if (ticket.getTicketId().equals(ticketId)) {
                ticket.setStatus(status);
                DatabaseAccess.updateSupportTicketStatus(ticketId, status);
                TICKETS.clear();
                TICKETS.addAll(DatabaseAccess.getAllSupportTickets());
                return true;
            }
        }
        return false;
    }

    public static boolean respondToTicket(String ticketId, String message) {
        TICKETS.clear();
        TICKETS.addAll(DatabaseAccess.getAllSupportTickets());
        for (SupportTicket ticket : TICKETS) {
            if (ticket.getTicketId().equals(ticketId)) {
                ticket.setResponse(message);
                ticket.setStatus("IN_PROGRESS");
                DatabaseAccess.updateSupportTicketResponse(ticketId, message, "IN_PROGRESS");
                notifyCreator(ticket, message);
                return true;
            }
        }
        return false;
    }

    private static void notifyCreator(SupportTicket ticket, String message) {
        Persona creator = PersonaService.getProfileByMemberId(ticket.getUserId());
        String subject = "Reply to ticket " + ticket.getTicketId();
        if (creator != null) {
            NotificationService.notify(creator, subject, message);
        } else {
            NotificationService.notifyAddress(ticket.getUserId(), subject, message);
        }
    }

    public static List<SupportTicket> getAllTickets() {
        TICKETS.clear();
        TICKETS.addAll(DatabaseAccess.getAllSupportTickets());
        return new ArrayList<>(TICKETS);
    }

    public static List<SupportTicket> getTicketsForAgent(Persona agent) {
        if (agent.getRole() != UserRole.CALLCENTER) {
            return getAllTickets();
        }
        
        java.util.Set<SupportSection> allowedSections = ((ir.ac.kntu.persona.CallCenterProfile) agent.getUserProfile()).getAssignedSections();
        List<SupportTicket> filtered = new ArrayList<>();
        for (SupportTicket ticket : getAllTickets()) {
            if (allowedSections.contains(ticket.getSection())) {
                filtered.add(ticket);
            }
        }
        return filtered;
    }

    public static boolean submitLibraryItemPlaceholder(String type, String title, String author) {
        return type != null && title != null && author != null;
    }

    public static void handleCallCenterStockUpdate(String itemId, int quantity) {
        Persona current = Persona.getCurrentUser();
        if (current != null && current.getRole() == UserRole.CALLCENTER) {
            LibraryService.updateItemQuantityFromCallCenter(itemId, quantity);
            System.out.println("[Support Bridge]: Verified agent request routed to inventory module.");
        } else {
            System.out.println("[Support Bridge Error]: Action denied. Unauthorized security clearance scope.");
        }
    }

    public static boolean addLibraryItemViaSupport(LibraryItem item) {
        Persona current = Persona.getCurrentUser();
        boolean allowed = current != null
                && (current.getRole() == UserRole.CALLCENTER || current.getRole() == UserRole.ADMIN);
        if (!allowed) {
            System.out.println("[Support Bridge Error]: Action denied. Unauthorized security clearance scope.");
            return false;
        }
        boolean added = LibraryService.addItem(item);
        if (added) {
            System.out.println("[Support Bridge]: New catalog item routed CallCenter -> Support -> Library.");
        }
        return added;
    }
}
