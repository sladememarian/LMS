package ir.ac.kntu.support.rolerequest;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.notification.NotificationService;

/**
 * Stores and processes Guest role-upgrade requests. Approval is delegated to
 * Persona (role change) and the outcome is announced through the Support
 * notification centre. Requests live for the session (in-memory registry).
 */
public class RoleRequestService {

    private static final List<RoleRequest> REQUESTS = new ArrayList<>();
    private static final String SUBJECT = "Role Request Update";

    public static RoleRequest submit(Persona requester, String requestedRole, String message) {
        String id = "RR-" + ((int) (Math.random() * 900_000) + 100_000);
        RoleRequest request = new RoleRequest(id, requester.getEmail(), requestedRole, message);
        REQUESTS.add(request);
        return request;
    }

    public static List<RoleRequest> getPending() {
        List<RoleRequest> pending = new ArrayList<>();
        for (RoleRequest request : REQUESTS) {
            if (RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
                pending.add(request);
            }
        }
        return pending;
    }

    public static List<RoleRequest> getAll() {
        return new ArrayList<>(REQUESTS);
    }

    public static boolean approve(String requestId) {
        RoleRequest request = find(requestId);
        if (request == null || !RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        request.setStatus(RoleRequest.STATUS_APPROVED);
        PersonaService.promoteRole(request.getRequesterEmail(), UserRole.valueOf(request.getRequestedRole()));
        NotificationService.notifyAddress(request.getRequesterEmail(), SUBJECT,
                "Approved: you are now " + request.getRequestedRole());
        return true;
    }

    public static boolean reject(String requestId) {
        RoleRequest request = find(requestId);
        if (request == null || !RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        request.setStatus(RoleRequest.STATUS_REJECTED);
        NotificationService.notifyAddress(request.getRequesterEmail(), SUBJECT,
                "Rejected: your " + request.getRequestedRole() + " request was declined");
        return true;
    }

    private static RoleRequest find(String requestId) {
        for (RoleRequest request : REQUESTS) {
            if (request.getRequestId().equals(requestId)) {
                return request;
            }
        }
        return null;
    }
}
