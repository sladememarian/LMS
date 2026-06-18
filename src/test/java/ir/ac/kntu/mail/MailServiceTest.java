package ir.ac.kntu.mail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailServiceTest {

    @Test
    void deliverAndVerifyTwoFactorCode() {
        String email = "twofa_" + System.nanoTime() + "@test.com";
        String code = MailService.deliver2FACode(email);
        assertFalse(MailService.verifyCode(email, "000000"), "Wrong code must fail");
        assertTrue(MailService.verifyCode(email, code), "Correct code must pass");
        assertFalse(MailService.verifyCode(email, code), "Code is single-use after success");
    }

    @Test
    void welcomeMessageLandsInInbox() {
        String email = "welcome_" + System.nanoTime() + "@test.com";
        MailService.sendWelcome(email);
        Inbox inbox = MailService.getInbox(email);
        List<MailMessage> messages = inbox.getMessages();
        assertEquals(1, messages.size());
        assertEquals(MessageType.WELCOME, messages.get(0).getMessageType());
    }

    @Test
    void systemNotificationAndMarkRead() {
        String email = "notif_" + System.nanoTime() + "@test.com";
        MailService.sendSystemNotification(email, "Subject", "Body");
        assertEquals(1, MailService.getInbox(email).getUnreadCount());
        MailService.markInboxRead(email);
        assertEquals(0, MailService.getInbox(email).getUnreadCount());
    }

    @Test
    void deleteInboxRemovesMessages() {
        String email = "delete_" + System.nanoTime() + "@test.com";
        MailService.sendWelcome(email);
        MailService.sendSystemNotification(email, "S", "B");
        int removed = MailService.deleteInbox(email);
        assertEquals(2, removed);
        assertTrue(MailService.getInbox(email).getMessages().isEmpty());
    }

    @Test
    void envConfigDefaults() {
        assertEquals("UniLibraryMail", MailService.getSystemName());
        assertEquals(100, MailService.getMaxMessages());
        assertEquals(5, MailService.getExpireMinutes());
    }

    @Test
    void inboxRespectsMailboxCap() {
        String email = "cap_" + System.nanoTime() + "@test.com";
        int max = MailService.getMaxMessages();
        for (int i = 0; i < max + 5; i++) {
            MailService.sendSystemNotification(email, "S" + i, "B" + i);
        }
        assertEquals(max, MailService.getInbox(email).getMessages().size());
    }
}
