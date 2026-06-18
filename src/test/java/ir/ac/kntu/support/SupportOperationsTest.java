package ir.ac.kntu.support;

import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.support.notification.NotificationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests ticket status transitions, the CallCenter -> Support -> Library add
 * bridge, and the Support notification centre (backed by Mail).
 */
class SupportOperationsTest {

    @Test
    void updateTicketStatusFindsAndUpdates() {
        String title = "Status case " + System.nanoTime();
        SupportService.createTicket("STU-100000", "Technical", title, "desc");
        String id = findId(title);
        assertTrue(SupportService.updateTicketStatus(id, "CLOSED"));
        assertFalse(SupportService.updateTicketStatus("TCK-000000", "CLOSED"));
    }

    @Test
    void addLibraryItemViaSupportRequiresOperatorRole() {
        Book book = new Book("ITEM-S" + (System.nanoTime() % 100_000), "Bridged", "Cat", 2021);
        book.setSupplierId("SUP-101");
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        book.setUnitPrice(50);
        Persona callCenter = PersonaService.getProfileByUsername("callcenter");
        Persona.setCurrentUser(callCenter);
        assertTrue(SupportService.addLibraryItemViaSupport(book));
        assertNotNull(LibraryService.getItemById(book.getItemId()));

        Persona.setCurrentUser(null);
        Book denied = new Book("ITEM-D" + (System.nanoTime() % 100_000), "Denied", "Cat", 2021);
        assertFalse(SupportService.addLibraryItemViaSupport(denied));
    }

    @Test
    void notificationsAreDeliveredThroughMail() {
        String address = "notif_" + System.nanoTime() + "@test.com";
        NotificationService.notifyAddress(address, "Role Update", "Approved");
        Inbox inbox = MailService.getInbox(address);
        boolean found = inbox.getMessages().stream()
                .anyMatch(message -> message.getMessageType() == MessageType.SYSTEM_NOTIFICATION);
        assertTrue(found);
    }

    private String findId(String title) {
        for (SupportTicket ticket : SupportService.getAllTickets()) {
            if (title.equals(ticket.getTitle())) {
                return ticket.getTicketId();
            }
        }
        throw new IllegalStateException("ticket not found");
    }
}
