package ir.ac.kntu.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportServiceTest {
    // Support tickets: digital papercuts that need attention

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
        // Printer is on fire? That's HIGH priority
        String title = "Printer issue " + System.nanoTime();
        SupportService.createTicket("STU-100000", ir.ac.kntu.support.SupportSection.TECHNICAL, title, "desc");
        assertEquals("HIGH", findByTitle(title).getPriority());
    }

    @Test
    void urgentKeywordEscalatesToCritical() {
        // URGENT in the title? CRITICAL it is
        String title = "URGENT outage " + System.nanoTime();
        SupportService.createTicket("STU-100000", ir.ac.kntu.support.SupportSection.BOOK_REQUEST, title, "desc");
        assertEquals("CRITICAL", findByTitle(title).getPriority());
    }

    @Test
    void respondStoresMessageAndMarksInProgress() {
        // "We are looking into it" - TM for generic response
        String title = "Need help " + System.nanoTime();
        SupportService.createTicket("STU-100000", ir.ac.kntu.support.SupportSection.BOOK_REQUEST, title, "desc");
        String ticketId = findByTitle(title).getTicketId();
        assertTrue(SupportService.respondToTicket(ticketId, "We are looking into it."));
        SupportTicket updated = findByTitle(title);
        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals("We are looking into it.", updated.getResponse());
        assertFalse(SupportService.respondToTicket("TCK-000000", "no such ticket"));
    }

    @Test
    void callCenterLoginValidates() {
        // callcenter@system.local / ccpass: the fort knox of credentials
        assertTrue(SupportService.validateCallCenterLogin("callcenter@system.local", "ccpass"));
        assertFalse(SupportService.validateCallCenterLogin("callcenter@system.local", "wrong"));
    }
}