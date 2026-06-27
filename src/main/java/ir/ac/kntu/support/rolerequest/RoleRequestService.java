package ir.ac.kntu.support.rolerequest;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.DatabaseAccess;

public class RoleRequestService {
    private static final List<RoleRequest> REQUESTS = new ArrayList<>();
    private static final String SUBJECT = "Role Request Update";

    static {
        REQUESTS.addAll(DatabaseAccess.getAllRoleRequests());
    }

    public static RoleRequest submit(Persona requester, String requestedRole, String message) {
        REQUESTS.clear();
        REQUESTS.addAll(DatabaseAccess.getAllRoleRequests());
        String id = "RR-" + ((int) (Math.random() * 900_000) + 100_000);
        RoleRequest request = new RoleRequest(id, requester.getEmail(), requestedRole, message);
        REQUESTS.add(request);
        DatabaseAccess.insertRoleRequest(request);
        return request;
    }

    public static List<RoleRequest> getPending() {
        REQUESTS.clear();
        REQUESTS.addAll(DatabaseAccess.getAllRoleRequests());
        List<RoleRequest> pending = new ArrayList<>();
        for (RoleRequest request : REQUESTS) {
            if (RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
                pending.add(request);
            }
        }
        return pending;
    }

    public static List<RoleRequest> getAll() {
        REQUESTS.clear();
        REQUESTS.addAll(DatabaseAccess.getAllRoleRequests());
        return new ArrayList<>(REQUESTS);
    }

    public static boolean approve(String requestId) {
        REQUESTS.clear();
        REQUESTS.addAll(DatabaseAccess.getAllRoleRequests());
        RoleRequest request = find(requestId);
        if (request == null || !RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        request.setStatus(RoleRequest.STATUS_APPROVED);
        DatabaseAccess.updateRoleRequestStatus(requestId, RoleRequest.STATUS_APPROVED);
        PersonaService.promoteRole(request.getRequesterEmail(), UserRole.valueOf(request.getRequestedRole()));
        NotificationService.notifyAddress(request.getRequesterEmail(), SUBJECT,
                "Approved: you are now " + request.getRequestedRole());
        return true;
    }

    public static boolean reject(String requestId) {
        REQUESTS.clear();
        REQUESTS.addAll(DatabaseAccess.getAllRoleRequests());
        RoleRequest request = find(requestId);
        if (request == null || !RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        request.setStatus(RoleRequest.STATUS_REJECTED);
        DatabaseAccess.updateRoleRequestStatus(requestId, RoleRequest.STATUS_REJECTED);
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
