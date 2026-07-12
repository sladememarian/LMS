package ir.ac.kntu.mail;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.util.EnvConfig;
import ir.ac.kntu.util.MailRepository;
import ir.ac.kntu.util.TwoFactorRepository;

public class MailService {
    private static final List<MailMessage> ALL_MESSAGES = new ArrayList<>();
    private static final long MILLIS_PER_MINUTE = 60_000L;

    static {
        ALL_MESSAGES.addAll(MailRepository.getAllMailMessages());
    }

    public static String getSystemName() {
        return EnvConfig.get("MAIL_SYSTEM_NAME", "UniLibraryMail");
    }

    public static int getMaxMessages() {
        return Integer.parseInt(EnvConfig.get("MAILBOX_MAX_MESSAGES", "100"));
    }

    public static int getExpireMinutes() {
        return Integer.parseInt(EnvConfig.get("SIMULATED_2FA_EXPIRE_MINUTES", "5"));
    }

    private static void ensureLoaded() {
        ALL_MESSAGES.clear();
        ALL_MESSAGES.addAll(MailRepository.getAllMailMessages());
    }

    public static MailMessage deliverMessage(String recipient, String subject, String body, MessageType type) {
        ensureLoaded();
        MailMessage message = new MailMessage(recipient, subject, body, type);
        ALL_MESSAGES.add(message);
        MailRepository.insertMailMessage(message);
        enforceMailboxCap(recipient);
        return message;
    }

    private static void enforceMailboxCap(String recipient) {
        int maxMessages = getMaxMessages();
        List<MailMessage> owned = new ArrayList<>();
        for (MailMessage message : ALL_MESSAGES) {
            if (message.getRecipientEmail().equalsIgnoreCase(recipient)) {
                owned.add(message);
            }
        }
        int removeCount = owned.size() - maxMessages;
        if (removeCount > 0) {
            for (int i = 0; i < removeCount; i++) {
                ALL_MESSAGES.remove(owned.get(i));
            }
            MailRepository.deleteMailMessagesForRecipient(recipient);
            for (MailMessage msg : ALL_MESSAGES) {
                if (msg.getRecipientEmail().equalsIgnoreCase(recipient)) {
                    MailRepository.insertMailMessage(msg);
                }
            }
        }
    }

    public static String deliver2FACode(String recipient) {
        String code = String.valueOf((int) (Math.random() * 900_000) + 100_000);
        TwoFactorRepository.saveTwoFactorCode(recipient.toLowerCase(), code, System.currentTimeMillis());
        String body = "Your " + getSystemName() + " verification code is " + code
                + ". It expires in " + getExpireMinutes() + " minutes.";
        deliverMessage(recipient, "Your 2FA Verification Code", body, MessageType.TWO_FA);
        return code;
    }

    public static boolean verifyCode(String recipient, String code) {
        String key = recipient.toLowerCase();
        String stored = TwoFactorRepository.getTwoFactorCode(key);
        Long issuedAt = TwoFactorRepository.getTwoFactorCodeIssuedAt(key);
        if (stored == null || issuedAt == null || !stored.equals(code)) {
            return false;
        }
        long ageMillis = System.currentTimeMillis() - issuedAt;
        boolean valid = ageMillis <= (long) getExpireMinutes() * MILLIS_PER_MINUTE;
        if (valid) {
            TwoFactorRepository.removeTwoFactorCode(key);
        }
        return valid;
    }

    public static void sendWelcome(String recipient) {
        String body = "Welcome to " + getSystemName() + "! Your account has been created successfully.";
        deliverMessage(recipient, "Welcome to " + getSystemName(), body, MessageType.WELCOME);
    }

    public static void sendPasswordReset(String recipient) {
        String body = "Your password was changed successfully. If this was not you, contact support immediately.";
        deliverMessage(recipient, "Password Changed", body, MessageType.PASSWORD_RESET);
    }

    public static void sendSystemNotification(String recipient, String subject, String body) {
        deliverMessage(recipient, subject, body, MessageType.SYSTEM_NOTIFICATION);
    }

    public static Inbox getInbox(String recipient) {
        ensureLoaded();
        Inbox inbox = new Inbox(recipient);
        int maxMessages = getMaxMessages();
        for (MailMessage message : ALL_MESSAGES) {
            if (message.getRecipientEmail().equalsIgnoreCase(recipient)) {
                inbox.addMessage(message, maxMessages);
            }
        }
        return inbox;
    }

    public static List<MailMessage> getAllMessages() {
        ensureLoaded();
        return new ArrayList<>(ALL_MESSAGES);
    }

    public static void markInboxRead(String recipient) {
        ensureLoaded();
        for (MailMessage message : ALL_MESSAGES) {
            if (message.getRecipientEmail().equalsIgnoreCase(recipient)) {
                message.setRead(true);
            }
        }
        MailRepository.markMailRead(recipient);
    }

    public static int deleteInbox(String recipient) {
        ensureLoaded();
        List<MailMessage> remaining = new ArrayList<>();
        int removed = 0;
        for (MailMessage message : ALL_MESSAGES) {
            if (message.getRecipientEmail().equalsIgnoreCase(recipient)) {
                removed++;
            } else {
                remaining.add(message);
            }
        }
        ALL_MESSAGES.clear();
        ALL_MESSAGES.addAll(remaining);
        MailRepository.deleteMailMessagesForRecipient(recipient);
        return removed;
    }
}
