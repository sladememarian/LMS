package ir.ac.kntu.mail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailServiceTest {
    // Mail service: because carrier pigeons are so 1800s

    @Test
    void deliverAndVerifyTwoFactorCode() {
        // 000000 is the 123456 of 2FA codes
        String email = "twofa_" + System.nanoTime() + "@test.com";
        String code = MailService.deliver2FACode(email);
        assertFalse(MailService.verifyCode(email, "000000"), "Wrong code must fail");
        assertTrue(MailService.verifyCode(email, code), "Correct code must pass");
        assertFalse(MailService.verifyCode(email, code), "Code is single-use after success");
    }

    @Test
    void welcomeMessageLandsInInbox() {
        // Welcome! Here be dragons and newsletters
        String email = "welcome_" + System.nanoTime() + "@test.com";
        MailService.sendWelcome(email);
        Inbox inbox = MailService.getInbox(email);
        List<MailMessage> messages = inbox.getMessages();
        assertEquals(1, messages.size());
        assertEquals(MessageType.WELCOME, messages.get(0).getMessageType());
    }

    @Test
    void systemNotificationAndMarkRead() {
        // Unread count: the anxiety meter
        String email = "notif_" + System.nanoTime() + "@test.com";
        MailService.sendSystemNotification(email, "Subject", "Body");
        assertEquals(1, MailService.getInbox(email).getUnreadCount());
        MailService.markInboxRead(email);
        assertEquals(0, MailService.getInbox(email).getUnreadCount());
    }

    @Test
    void deleteInboxRemovesMessages() {
        // Delete all the things! (with prejudice)
        String email = "delete_" + System.nanoTime() + "@test.com";
        MailService.sendWelcome(email);
        MailService.sendSystemNotification(email, "S", "B");
        int removed = MailService.deleteInbox(email);
        assertEquals(2, removed);
        assertTrue(MailService.getInbox(email).getMessages().isEmpty());
    }

    @Test
    void envConfigDefaults() {
        // Config defaults: the answer to "what if we don't set it?"
        assertEquals("UniLibraryMail", MailService.getSystemName());
        assertEquals(100, MailService.getMaxMessages());
        assertEquals(5, MailService.getExpireMinutes());
    }

    @Test
    void inboxRespectsMailboxCap() {
        // Mailbox full? That's a tomorrow problem
        String email = "cap_" + System.nanoTime() + "@test.com";
        int max = MailService.getMaxMessages();
        for (int i = 0; i < max + 5; i++) {
            MailService.sendSystemNotification(email, "S" + i, "B" + i);
        }
        assertEquals(max, MailService.getInbox(email).getMessages().size());
    }
}