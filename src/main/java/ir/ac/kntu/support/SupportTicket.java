package ir.ac.kntu.support;

import ir.ac.kntu.interfaces.Displayable;
import ir.ac.kntu.util.ConsoleColor;

public class SupportTicket implements Comparable<SupportTicket>, Displayable {
    // each ticket is someone's cry for help
    private final String ticketId;
    private final String userId;
    private String title;
    private String description;
    private SupportSection section;
    private String priority;
    private String status;
    private String response;

    public SupportTicket(String ticketId, String userId, String title, String description, SupportSection section) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.section = section;
        this.priority = "LOW";
        this.status = "Open";
        this.response = "";
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public SupportSection getSection() {
        return section;
    }

    public void setSection(SupportSection section) {
        this.section = section;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    private int getPriorityValue() {
        switch (priority.toLowerCase()) {
            case "critical":
                return 4;
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            default:
                return 0;
        }
    }

    @Override
    public int compareTo(SupportTicket other) {
        return Integer.compare(other.getPriorityValue(), this.getPriorityValue());
    }

    @Override
    public String toDisplayString() {
        String line = ConsoleColor.CYAN + "  " + ticketId + ConsoleColor.RESET
                + " | " + title
                + ConsoleColor.gray(" [" + section + "/" + priority + "] " + status);
        if (response != null && !response.isEmpty()) {
            line += "\n" + ConsoleColor.gray("      Support reply: " + response);
        }
        return line;
    }
}