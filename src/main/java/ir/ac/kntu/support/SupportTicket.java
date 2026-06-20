package ir.ac.kntu.support;

public class SupportTicket implements Comparable<SupportTicket> {
    private final String ticketId;
    private final String userId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String response;

    public SupportTicket(String ticketId, String userId, String title, String description) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.category = "General";
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

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
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

    public void setCategory(String category) {
        this.category = category;
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

}