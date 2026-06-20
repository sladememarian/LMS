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

/**
 * Guest -> Support -> Admin -> Persona role-request workflow. Because requests
 * are now persisted and reloaded on every call, a request raised in one
 * instance is visible to a separate Admin instance; these tests assert against
 * the reloaded view (look up by request id) rather than object identity.
 */
class RoleRequestServiceTest {

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
        Persona guest = freshGuest();
        RoleRequest request = RoleRequestService.submit(guest, UserRole.STUDENT.name(), "please");
        assertTrue(RoleRequestService.approve(request.getRequestId()));
        assertEquals(UserRole.STUDENT, PersonaService.getProfile(guest.getEmail()).getRole());
        assertFalse(RoleRequestService.approve(request.getRequestId()), "already processed");
    }

    @Test
    void rejectMarksRequestRejected() {
        Persona guest = freshGuest();
        RoleRequest request = RoleRequestService.submit(guest, UserRole.TEACHER.name(), "please");
        assertTrue(RoleRequestService.reject(request.getRequestId()));
        RoleRequest reloaded = reloadById(request.getRequestId());
        assertNotNull(reloaded);
        assertEquals(RoleRequest.STATUS_REJECTED, reloaded.getStatus());
        assertFalse(RoleRequestService.reject("RR-000000"));
    }
}
