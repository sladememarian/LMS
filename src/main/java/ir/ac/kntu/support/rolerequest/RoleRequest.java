package ir.ac.kntu.support.rolerequest;

/**
 * A request from a Guest to be upgraded to Student or Teacher. Created in
 * Support, reviewed in the Admin inbox, and (if approved) applied in Persona.
 */
public class RoleRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final String requestId;
    private final String requesterEmail;
    private final String requestedRole;
    private final String message;
    private String status;

    public RoleRequest(String requestId, String requesterEmail, String requestedRole, String message) {
        this.requestId = requestId;
        this.requesterEmail = requesterEmail;
        this.requestedRole = requestedRole;
        this.message = message;
        this.status = STATUS_PENDING;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
