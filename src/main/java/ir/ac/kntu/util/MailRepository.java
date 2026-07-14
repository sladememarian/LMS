package ir.ac.kntu.util;

import java.util.List;

import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MessageType;

// Persistence for the mail_messages table. Split out of the former
// monolithic DatabaseAccess class as part of the per-domain
// repository migration.
public final class MailRepository {

    private MailRepository() {
    }

    public static void clearMailMessages() {
        Database.executeUpdate("DELETE FROM mail_messages");
    }

    public static void insertMailMessage(MailMessage msg) {
        Database.withPs("INSERT INTO mail_messages (message_id, recipient_email, subject, body, type, sent_date, is_read) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, msg.getMessageId());
            ps.setString(2, msg.getRecipientEmail());
            ps.setString(3, msg.getSubject());
            ps.setString(4, msg.getBody());
            ps.setString(5, msg.getMessageType().getLabel());
            ps.setString(6, msg.getSentDate());
            ps.setBoolean(7, msg.isRead());
            ps.executeUpdate();
        });
    }

    public static List<MailMessage> getAllMailMessages() {
        return Database.queryAll("SELECT * FROM mail_messages", rs -> {
            MailMessage msg = new MailMessage(rs.getString("recipient_email"), rs.getString("subject"),
                    rs.getString("body"), MessageType.fromLabel(rs.getString("type")));
            msg.setMessageId(rs.getString("message_id"));
            msg.setSentDate(rs.getString("sent_date"));
            msg.setRead(rs.getBoolean("is_read"));
            return msg;
        });
    }

    public static void deleteMailMessagesForRecipient(String recipient) {
        Database.withPs("DELETE FROM mail_messages WHERE recipient_email=?", ps -> {
            ps.setString(1, recipient);
            ps.executeUpdate();
        });
    }

    public static void markMailRead(String recipient) {
        Database.withPs("UPDATE mail_messages SET is_read=TRUE WHERE recipient_email=?", ps -> {
            ps.setString(1, recipient);
            ps.executeUpdate();
        });
    }
}
