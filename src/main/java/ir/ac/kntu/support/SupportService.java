package ir.ac.kntu.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.ac.kntu.exception.AuthorizationException;
import ir.ac.kntu.exception.NotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.SupportTicketRepository;

public class SupportService {
    private static final List<SupportTicket> TICKETS = new ArrayList<>();

    static {
        TICKETS.addAll(SupportTicketRepository.getAllSupportTickets());
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
        validateTicketFields(title, description);
        TICKETS.clear();
        TICKETS.addAll(SupportTicketRepository.getAllSupportTickets());
        String priority = resolvePriority(section, title);

        String id = "TCK-" + ((int) (Math.random() * 900_000) + 100_000);
        SupportTicket ticket = new SupportTicket(id, userId, title, description, section);
        ticket.setPriority(priority);
        ticket.setStatus("OPEN");
        TICKETS.add(ticket);
        Collections.sort(TICKETS);
        SupportTicketRepository.insertSupportTicket(ticket);
    }

    private static void validateTicketFields(String title, String description) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Ticket title cannot be empty.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Ticket description cannot be empty.");
        }
    }

    private static String resolvePriority(SupportSection section, String title) {
        if (SupportSection.TECHNICAL == section) {
            return "HIGH";
        }
        String upperTitle = title.toUpperCase();
        if (upperTitle.contains("URGENT") || upperTitle.contains("CRASH")
                || upperTitle.contains("BLOCK")) {
            return "CRITICAL";
        }
        return "LOW";
    }

    public static void updateTicketStatus(String ticketId, String status) {
        TICKETS.clear();
        TICKETS.addAll(SupportTicketRepository.getAllSupportTickets());
        for (SupportTicket ticket : TICKETS) {
            if (ticket.getTicketId().equals(ticketId)) {
                ticket.setStatus(status);
                SupportTicketRepository.updateSupportTicketStatus(ticketId, status);
                TICKETS.clear();
                TICKETS.addAll(SupportTicketRepository.getAllSupportTickets());
                return;
            }
        }
        throw new NotFoundException("Ticket not found: " + ticketId);
    }

    public static void respondToTicket(String ticketId, String message) {
        TICKETS.clear();
        TICKETS.addAll(SupportTicketRepository.getAllSupportTickets());
        for (SupportTicket ticket : TICKETS) {
            if (ticket.getTicketId().equals(ticketId)) {
                ticket.setResponse(message);
                ticket.setStatus("IN_PROGRESS");
                SupportTicketRepository.updateSupportTicketResponse(ticketId, message, "IN_PROGRESS");
                notifyCreator(ticket, message);
                return;
            }
        }
        throw new NotFoundException("Ticket not found: " + ticketId);
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
        TICKETS.addAll(SupportTicketRepository.getAllSupportTickets());
        return new ArrayList<>(TICKETS);
    }

    public static List<SupportTicket> getTicketsForAgent(Persona agent) {
        if (agent.getRole() != UserRole.CALLCENTER) {
            return getAllTickets();
        }

        TICKETS.clear();
        TICKETS.addAll(SupportTicketRepository.getAllSupportTickets());

        java.util.Set<SupportSection> allowedSections = agent.getAssignedSupportSections();
        List<SupportTicket> filtered = new ArrayList<>();
        for (SupportTicket ticket : TICKETS) {
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
        if (current == null || current.getRole() != UserRole.CALLCENTER) {
            throw new AuthorizationException("Action denied. Unauthorized security clearance scope.");
        }
        LibraryService.updateItemQuantityFromCallCenter(itemId, quantity);
    }

    public static void addLibraryItemViaSupport(LibraryItem item) {
        Persona current = Persona.getCurrentUser();
        boolean allowed = current != null
                && (current.getRole() == UserRole.CALLCENTER || current.getRole() == UserRole.ADMIN);
        if (!allowed) {
            throw new AuthorizationException("Action denied. Unauthorized security clearance scope.");
        }
        LibraryService.addItem(item);
    }
}
