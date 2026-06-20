package ir.ac.kntu.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailModelsTest {
    // Mail models: the atoms of electronic carrier pigeonry

    @Test
    void messageTypeLabelRoundTrip() {
        // Enum round-trip: there and back again
        assertEquals("2FA", MessageType.TWO_FA.getLabel());
        assertEquals(MessageType.WELCOME, MessageType.fromLabel("WELCOME"));
        assertEquals(MessageType.SYSTEM_NOTIFICATION, MessageType.fromLabel("unknown-label"));
    }

    @Test
    void mailMessageDefaults() {
        // Fresh message, unread, unloved, with a fresh MSG- prefix
        MailMessage message = new MailMessage("a@b.com", "Hi", "Body", MessageType.WELCOME);
        assertFalse(message.isRead());
        assertTrue(message.getMessageId().startsWith("MSG-"));
        assertEquals("a@b.com", message.getRecipientEmail());
        message.setRead(true);
        assertTrue(message.isRead());
    }

    @Test
    void inboxBoundedAndCountsUnread() {
        // Bounded inbox: because infinite storage is someone else's problem
        Inbox inbox = new Inbox("a@b.com");
        for (int i = 0; i < 5; i++) {
            inbox.addMessage(new MailMessage("a@b.com", "S", "B", MessageType.SYSTEM_NOTIFICATION), 3);
        }
        assertEquals(3, inbox.getMessages().size());
        assertEquals(3, inbox.getUnreadCount());
    }
}