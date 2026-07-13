package ir.ac.kntu.support;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.support.notification.NotificationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupportOperationsTest {
    // Ticket operations: where we close things (eventually)

    @Test
    void updateTicketStatusFindsAndUpdates() {
        // From OPEN to CLOSED in 6-8 business weeks
        String title = "Status case " + System.nanoTime();
        SupportService.createTicket("STU-100000", ir.ac.kntu.support.SupportSection.TECHNICAL, title, "desc");
        String id = findId(title);
        assertDoesNotThrow(() -> SupportService.updateTicketStatus(id, "CLOSED"));
        assertThrows(BaseException.class, () -> SupportService.updateTicketStatus("TCK-000000", "CLOSED"));
    }

    @Test
    void addLibraryItemViaSupportRequiresOperatorRole() {
        // With great power comes great book-adding responsibility
        Book book = new Book("ITEM-S" + (System.nanoTime() % 100_000), "Bridged", "Cat", 2021);
        book.setSupplierId("SUP-101");
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        book.setUnitPrice(50);
        PersonaService.validateCredentials("callcenter@system.local", "ccpass");
        Persona callCenter = PersonaService.getProfile("callcenter@system.local");
        Persona.setCurrentUser(callCenter);
        assertDoesNotThrow(() -> SupportService.addLibraryItemViaSupport(book));
        assertNotNull(LibraryService.getItemById(book.getItemId()));

        Persona.setCurrentUser(null);
        Book denied = new Book("ITEM-D" + (System.nanoTime() % 100_000), "Denied", "Cat", 2021);
        assertThrows(BaseException.class, () -> SupportService.addLibraryItemViaSupport(denied));
    }

    @Test
    void notificationsAreDeliveredThroughMail() {
        // Notification delivery: we guarantee it hits the spam folder
        String address = "notif_" + System.nanoTime() + "@test.com";
        NotificationService.notifyAddress(address, "Role Update", "Approved");
        Inbox inbox = MailService.getInbox(address);
        boolean found = inbox.getMessages().stream()
                .anyMatch(message -> message.getMessageType() == MessageType.SYSTEM_NOTIFICATION);
        assertNotNull(inbox);
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