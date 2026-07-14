package ir.ac.kntu.util;

import java.util.List;

import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.support.SupportTicket;

// Persistence for the support_tickets table. Split out of the former
// monolithic DatabaseAccess class as part of the per-domain
// repository migration.
public final class SupportTicketRepository {

    private SupportTicketRepository() {
    }

    public static void clearSupportTickets() {
        Database.executeUpdate("DELETE FROM support_tickets");
    }

    public static void insertSupportTicket(SupportTicket ticket) {
        Database.withPs("MERGE INTO support_tickets USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?)) AS s(ticket_id, user_id, title, description, section, priority, status, response) ON support_tickets.ticket_id = s.ticket_id WHEN MATCHED THEN UPDATE SET user_id = s.user_id, title = s.title, description = s.description, section = s.section, priority = s.priority, status = s.status, response = s.response WHEN NOT MATCHED THEN INSERT (ticket_id, user_id, title, description, section, priority, status, response) VALUES (s.ticket_id, s.user_id, s.title, s.description, s.section, s.priority, s.status, s.response)", ps -> {
            ps.setString(1, ticket.getTicketId());
            ps.setString(2, ticket.getUserId());
            ps.setString(3, ticket.getTitle());
            ps.setString(4, ticket.getDescription());
            ps.setString(5, ticket.getSection().name());
            ps.setString(6, ticket.getPriority());
            ps.setString(7, ticket.getStatus());
            ps.setString(8, ticket.getResponse());
            ps.executeUpdate();
        });
    }

    public static List<SupportTicket> getAllSupportTickets() {
        return Database.queryAll("SELECT * FROM support_tickets", rs -> {
            SupportTicket ticket = new SupportTicket(rs.getString("ticket_id"), rs.getString("user_id"),
                    rs.getString("title"), rs.getString("description"),
                    SupportSection.valueOf(rs.getString("section")));
            ticket.setPriority(rs.getString("priority"));
            ticket.setStatus(rs.getString("status"));
            ticket.setResponse(rs.getString("response"));
            return ticket;
        });
    }

    public static void updateSupportTicketStatus(String ticketId, String status) {
        Database.withPs("UPDATE support_tickets SET status=? WHERE ticket_id=?", ps -> {
            ps.setString(1, status);
            ps.setString(2, ticketId);
            ps.executeUpdate();
        });
    }

    public static void updateSupportTicketResponse(String ticketId, String response, String status) {
        Database.withPs("UPDATE support_tickets SET response=?, status=? WHERE ticket_id=?", ps -> {
            ps.setString(1, response);
            ps.setString(2, status);
            ps.setString(3, ticketId);
            ps.executeUpdate();
        });
    }
}
