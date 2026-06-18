package ir.ac.kntu.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.ac.kntu.util.EnvConfig;

public class MailService {

    private static final List<MailMessage> ALL_MESSAGES = new ArrayList<>();
    private static final Map<String, String> ACTIVE_CODES = new HashMap<>();
    private static final Map<String, Long> CODE_ISSUED_AT = new HashMap<>();
    private static final String FILE_PATH = "mail.enc";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_BODY = "body";
    private static final String KEY_TYPE = "type";
    private static final String KEY_DATE = "date";
    private static final String KEY_READ = "read";
    private static final String KEY_ID = "mid";
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static boolean loaded;

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
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
        if (!loaded) {
            loadFromEncryptedFile();
            loaded = true;
        }
    }

    public static MailMessage deliverMessage(String recipient, String subject, String body, MessageType type) {
        ensureLoaded();
        MailMessage message = new MailMessage(recipient, subject, body, type);
        ALL_MESSAGES.add(message);
        enforceMailboxCap(recipient);
        saveToEncryptedFile();
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
        for (int i = 0; i < removeCount; i++) {
            ALL_MESSAGES.remove(owned.get(i));
        }
    }

    public static String deliver2FACode(String recipient) {
        String code = String.valueOf((int) (Math.random() * 900_000) + 100_000);
        ACTIVE_CODES.put(recipient.toLowerCase(), code);
        CODE_ISSUED_AT.put(recipient.toLowerCase(), System.currentTimeMillis());
        String body = "Your " + getSystemName() + " verification code is " + code
                + ". It expires in " + getExpireMinutes() + " minutes.";
        deliverMessage(recipient, "Your 2FA Verification Code", body, MessageType.TWO_FA);
        return code;
    }

    public static boolean verifyCode(String recipient, String code) {
        String key = recipient.toLowerCase();
        String stored = ACTIVE_CODES.get(key);
        Long issuedAt = CODE_ISSUED_AT.get(key);
        if (stored == null || issuedAt == null || !stored.equals(code)) {
            return false;
        }
        long ageMillis = System.currentTimeMillis() - issuedAt;
        boolean valid = ageMillis <= (long) getExpireMinutes() * MILLIS_PER_MINUTE;
        if (valid) {
            ACTIVE_CODES.remove(key);
            CODE_ISSUED_AT.remove(key);
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
        saveToEncryptedFile();
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
        saveToEncryptedFile();
        return removed;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\"", "'").replace("\n", " ");
    }

    private static void appendMessage(StringBuilder builder, MailMessage message) {
        String suffix = "\",\n";
        builder.append("  {\n")
                .append("    \"mid\": \"").append(message.getMessageId()).append(suffix)
                .append("    \"email\": \"").append(message.getRecipientEmail()).append(suffix)
                .append("    \"subject\": \"").append(escape(message.getSubject())).append(suffix)
                .append("    \"body\": \"").append(escape(message.getBody())).append(suffix)
                .append("    \"type\": \"").append(message.getMessageType().getLabel()).append(suffix)
                .append("    \"date\": \"").append(message.getSentDate()).append(suffix)
                .append("    \"read\": \"").append(message.isRead()).append("\"\n")
                .append("  }");
    }

    private static void saveToEncryptedFile() {
        StringBuilder builder = new StringBuilder("[\n");
        for (int i = 0; i < ALL_MESSAGES.size(); i++) {
            appendMessage(builder, ALL_MESSAGES.get(i));
            if (i < ALL_MESSAGES.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("]");
        byte[] keyBytes = getEncryptionKey();
        byte[] rawBytes = builder.toString().getBytes();
        byte[] encrypted = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            encrypted[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(encrypted);
        } catch (IOException ex) {
            System.err.println("Error saving mail data: " + ex.getMessage());
        }
    }

    private static void loadFromEncryptedFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] encrypted = fis.readAllBytes();
            byte[] keyBytes = getEncryptionKey();
            byte[] decrypted = new byte[encrypted.length];
            for (int i = 0; i < encrypted.length; i++) {
                decrypted[i] = (byte) (encrypted[i] ^ keyBytes[i % keyBytes.length]);
            }
            parseMailJson(new String(decrypted));
        } catch (IOException ex) {
            System.err.println("Error loading mail data: " + ex.getMessage());
        }
    }

    private static void parseMailJson(String raw) {
        ALL_MESSAGES.clear();
        String clean = raw.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        String[] blocks = clean.split("\\},");
        for (String block : blocks) {
            String obj = block.replace("{", "").replace("}", "").trim();
            if (obj.contains("\"" + KEY_ID + "\":")) {
                ALL_MESSAGES.add(reconstructMessage(obj));
            }
        }
    }

    private static MailMessage reconstructMessage(String obj) {
        String recipient = extract(obj, KEY_EMAIL);
        String subject = extract(obj, KEY_SUBJECT);
        String body = extract(obj, KEY_BODY);
        MessageType type = MessageType.fromLabel(extract(obj, KEY_TYPE));
        MailMessage message = new MailMessage(recipient, subject, body, type);
        message.setMessageId(extract(obj, KEY_ID));
        message.setSentDate(extract(obj, KEY_DATE));
        message.setRead(Boolean.parseBoolean(extract(obj, KEY_READ)));
        return message;
    }

    private static String extract(String src, String key) {
        String token = "\"" + key + "\": \"";
        int start = src.indexOf(token);
        if (start == -1) {
            return "";
        }
        start += token.length();
        return src.substring(start, src.indexOf("\"", start));
    }
}
