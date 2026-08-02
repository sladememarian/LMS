package ir.ac.kntu.support.rolerequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ir.ac.kntu.exception.ConflictException;
import ir.ac.kntu.exception.NotFoundException;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.PersonaRepository;
import ir.ac.kntu.util.RoleRequestRepository;

public class RoleRequestService {
    private static final List<RoleRequest> REQUESTS = new ArrayList<>();
    private static final String SUBJECT = "Role Request Update";
    private static final String ADMIN_SUBJECT = "New Role Request";

    static {
        REQUESTS.addAll(RoleRequestRepository.getAllRoleRequests());
    }

    public static synchronized RoleRequest submit(Persona requester, String requestedRole, String message) {
        REQUESTS.clear();
        REQUESTS.addAll(RoleRequestRepository.getAllRoleRequests());
        String id = "RR-" + ((int) (Math.random() * 900_000) + 100_000);
        RoleRequest request = new RoleRequest(id, requester.getEmail(), requestedRole, message);
        REQUESTS.add(request);
        RoleRequestRepository.insertRoleRequest(request);
        // Notify every admin so the request shows up in their Notifications tab,
        // not only on the dashboard. Previously submit() notified nobody, so
        // admins had no inbox record of pending requests.
        notifyAdmins(request);
        return request;
    }

    private static void notifyAdmins(RoleRequest request) {
        String body = request.getRequesterEmail() + " requested the "
                + request.getRequestedRole() + " role"
                + (request.getMessage() == null || request.getMessage().isBlank()
                        ? "." : ": " + request.getMessage());
        for (Persona persona : PersonaRepository.getAllPersonas()) {
            if (persona.getRole() == UserRole.ADMIN && persona.getEmail() != null) {
                NotificationService.notifyAddress(persona.getEmail(), ADMIN_SUBJECT, body);
            }
        }
    }

    public static synchronized List<RoleRequest> getPending() {
        REQUESTS.clear();
        REQUESTS.addAll(RoleRequestRepository.getAllRoleRequests());
        return REQUESTS.stream()
                .filter(r -> RoleRequest.STATUS_PENDING
                        .equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    public static synchronized List<RoleRequest> getAll() {
        REQUESTS.clear();
        REQUESTS.addAll(RoleRequestRepository.getAllRoleRequests());
        return new ArrayList<>(REQUESTS);
    }

    public static synchronized void approve(String requestId) {
        REQUESTS.clear();
        REQUESTS.addAll(RoleRequestRepository.getAllRoleRequests());
        RoleRequest request = find(requestId);
        if (request == null) {
            throw new NotFoundException("Role request not found: " + requestId);
        }
        if (!RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new ConflictException("Role request " + requestId + " has already been processed.");
        }
        request.setStatus(RoleRequest.STATUS_APPROVED);
        RoleRequestRepository.updateRoleRequestStatus(requestId, RoleRequest.STATUS_APPROVED);
        PersonaService.promoteRole(request.getRequesterEmail(), UserRole.valueOf(request.getRequestedRole()));
        NotificationService.notifyAddress(request.getRequesterEmail(), SUBJECT,
                "Approved: you are now " + request.getRequestedRole());
    }

    public static synchronized void reject(String requestId) {
        REQUESTS.clear();
        REQUESTS.addAll(RoleRequestRepository.getAllRoleRequests());
        RoleRequest request = find(requestId);
        if (request == null) {
            throw new NotFoundException("Role request not found: " + requestId);
        }
        if (!RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new ConflictException("Role request " + requestId + " has already been processed.");
        }
        request.setStatus(RoleRequest.STATUS_REJECTED);
        RoleRequestRepository.updateRoleRequestStatus(requestId, RoleRequest.STATUS_REJECTED);
        NotificationService.notifyAddress(request.getRequesterEmail(), SUBJECT,
                "Rejected: your " + request.getRequestedRole() + " request was declined");
    }

    private static RoleRequest find(String requestId) {
        return REQUESTS.stream()
                .filter(r -> r.getRequestId().equals(requestId))
                .findFirst()
                .orElse(null);
    }
}
