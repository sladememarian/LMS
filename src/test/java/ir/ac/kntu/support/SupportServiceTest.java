package ir.ac.kntu.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Support ticket priority rules, the call-centre login, and the feature where
 * a call-centre reply both stores a message on the ticket and moves it to
 * IN_PROGRESS.
 */
class SupportServiceTest {

    private SupportTicket findByTitle(String title) {
        for (SupportTicket ticket : SupportService.getAllTickets()) {
            if (title.equals(ticket.getTitle())) {
                return ticket;
            }
        }
        throw new IllegalStateException("ticket not found");
    }

    @Test
    void technicalTicketIsHighPriority() {
        String title = "Printer issue " + System.nanoTime();
        SupportService.createTicket("STU-100000", "Technical", title, "desc");
        assertEquals("HIGH", findByTitle(title).getPriority());
    }

    @Test
    void urgentKeywordEscalatesToCritical() {
        String title = "URGENT outage " + System.nanoTime();
        SupportService.createTicket("STU-100000", "General", title, "desc");
        assertEquals("CRITICAL", findByTitle(title).getPriority());
    }

    @Test
    void respondStoresMessageAndMarksInProgress() {
        String title = "Need help " + System.nanoTime();
        SupportService.createTicket("STU-100000", "General", title, "desc");
        String ticketId = findByTitle(title).getTicketId();
        assertTrue(SupportService.respondToTicket(ticketId, "We are looking into it."));
        SupportTicket updated = findByTitle(title);
        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals("We are looking into it.", updated.getResponse());
        assertFalse(SupportService.respondToTicket("TCK-000000", "no such ticket"));
    }

    @Test
    void callCenterLoginValidates() {
        assertTrue(SupportService.validateCallCenterLogin("callcenter", "ccpass"));
        assertFalse(SupportService.validateCallCenterLogin("callcenter", "wrong"));
    }
}
