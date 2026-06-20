package ir.ac.kntu.support;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.rolerequest.RoleRequest;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleRequestServiceTest {
    // Role requests: "Mother, may I?" for permissions

    private Persona freshGuest() {
        String email = "rr_" + System.nanoTime() + "@test.com";
        return PersonaService.registerPersona(email, "Passw0rd!");
    }

    private RoleRequest reloadById(String requestId) {
        for (RoleRequest request : RoleRequestService.getAll()) {
            if (request.getRequestId().equals(requestId)) {
                return request;
            }
        }
        return null;
    }

    @Test
    void submittedRequestIsVisibleAfterReload() {
        // Did you save? Did you? Did you reload?
        Persona guest = freshGuest();
        RoleRequest request = RoleRequestService.submit(guest, UserRole.STUDENT.name(), "please");
        boolean foundPending = false;
        for (RoleRequest pending : RoleRequestService.getPending()) {
            if (pending.getRequestId().equals(request.getRequestId())) {
                foundPending = true;
                assertEquals(RoleRequest.STATUS_PENDING, pending.getStatus());
            }
        }
        assertTrue(foundPending, "a freshly submitted request must be visible to another reader");
    }

    @Test
    void approveUpgradesPersonaRole() {
        // You get a role! You get a role! EVERYONE GETS... oh wait, rejected
        Persona guest = freshGuest();
        RoleRequest request = RoleRequestService.submit(guest, UserRole.STUDENT.name(), "please");
        assertTrue(RoleRequestService.approve(request.getRequestId()));
        assertEquals(UserRole.STUDENT, PersonaService.getProfile(guest.getEmail()).getRole());
        assertFalse(RoleRequestService.approve(request.getRequestId()), "already processed");
    }

    @Test
    void rejectMarksRequestRejected() {
        // Rejected: the "thanks for playing" of role requests
        Persona guest = freshGuest();
        RoleRequest request = RoleRequestService.submit(guest, UserRole.TEACHER.name(), "please");
        assertTrue(RoleRequestService.reject(request.getRequestId()));
        RoleRequest reloaded = reloadById(request.getRequestId());
        assertNotNull(reloaded);
        assertEquals(RoleRequest.STATUS_REJECTED, reloaded.getStatus());
        assertFalse(RoleRequestService.reject("RR-000000"));
    }
}