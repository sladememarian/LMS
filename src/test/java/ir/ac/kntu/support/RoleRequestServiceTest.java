package ir.ac.kntu.support;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.rolerequest.RoleRequest;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Guest -> Support -> Admin -> Persona role-request workflow.
 */
class RoleRequestServiceTest {

    private Persona freshGuest() {
        String email = "rr_" + System.nanoTime() + "@test.com";
        return PersonaService.registerPersona(email, "Passw0rd!");
    }

    @Test
    void submitCreatesPendingRequest() {
        Persona guest = freshGuest();
        RoleRequest request = RoleRequestService.submit(guest, UserRole.STUDENT.name(), "please");
        assertEquals(RoleRequest.STATUS_PENDING, request.getStatus());
        assertTrue(RoleRequestService.getPending().contains(request));
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
    void rejectMarksRejected() {
        Persona guest = freshGuest();
        RoleRequest request = RoleRequestService.submit(guest, UserRole.TEACHER.name(), "please");
        assertTrue(RoleRequestService.reject(request.getRequestId()));
        assertEquals(RoleRequest.STATUS_REJECTED, request.getStatus());
        assertFalse(RoleRequestService.reject("RR-000000"));
    }
}
