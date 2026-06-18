package ir.ac.kntu.support;

import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void generalTicketIsLowPriority() {
        String title = "Question " + System.nanoTime();
        SupportService.createTicket("STU-100000", "General", title, "desc");
        assertEquals("LOW", findByTitle(title).getPriority());
    }

    @Test
    void ticketsSortedByPriorityDescending() {
        List<SupportTicket> tickets = SupportService.getAllTickets();
        for (int i = 1; i < tickets.size(); i++) {
            assertTrue(tickets.get(i - 1).compareTo(tickets.get(i)) <= 0);
        }
    }

    @Test
    void callCenterLoginValidates() {
        assertTrue(SupportService.validateCallCenterLogin("callcenter", "ccpass"));
        assertFalse(SupportService.validateCallCenterLogin("callcenter", "wrong"));
    }

    @Test
    void stockUpdateRequiresCallCenterRole() {
        LibraryItem item = LibraryService.getAllItems().get(0);
        String id = item.getItemId();
        int before = current(id).getTotalCopies();
        Persona callCenter = PersonaService.getProfileByUsername("callcenter");
        Persona.setCurrentUser(callCenter);
        SupportService.handleCallCenterStockUpdate(id, 2);
        assertEquals(before + 2, current(id).getTotalCopies());

        Persona.setCurrentUser(null);
        SupportService.handleCallCenterStockUpdate(id, 5);
        assertEquals(before + 2, current(id).getTotalCopies());
    }

    @Test
    void placeholderRejectsNulls() {
        assertTrue(SupportService.submitLibraryItemPlaceholder("BOOK", "T", "A"));
        assertFalse(SupportService.submitLibraryItemPlaceholder(null, "T", "A"));
    }

    private LibraryItem current(String id) {
        for (LibraryItem item : LibraryService.getAllItems()) {
            if (item.getItemId().equals(id)) {
                return item;
            }
        }
        throw new IllegalStateException("missing");
    }
}
