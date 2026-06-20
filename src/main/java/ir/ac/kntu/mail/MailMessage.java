package ir.ac.kntu.mail;

import java.time.LocalDateTime;

public class MailMessage {

    private String messageId;
    private final String recipientEmail;
    private final String subject;
    private final String body;
    private String sentDate;
    private boolean read;
    private MessageType messageType;

    public MailMessage(String recipientEmail, String subject, String body, MessageType messageType) {
        this.messageId = "MSG-" + ((int) (Math.random() * 900_000) + 100_000);
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.messageType = messageType;
        this.sentDate = LocalDateTime.now().toString();
        this.read = false;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getSentDate() {
        return sentDate;
    }

    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}