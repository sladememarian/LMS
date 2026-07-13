package ir.ac.kntu.mail;

import java.util.ArrayList;
import java.util.List;

public class Inbox {

    private final String emailAddress;
    private final List<MailMessage> messages;

    public Inbox(String emailAddress) {
        this.emailAddress = emailAddress;
        this.messages = new ArrayList<>();
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public List<MailMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public void addMessage(MailMessage message, int maxMessages) {
        messages.add(message);
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    public int getUnreadCount() {
        return (int) messages.stream()
                .filter(m -> !m.isRead())
                .count();
    }
}