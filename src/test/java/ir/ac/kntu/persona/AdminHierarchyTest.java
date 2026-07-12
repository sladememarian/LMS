package ir.ac.kntu.persona;

import ir.ac.kntu.exception.AuthorizationException;
import ir.ac.kntu.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminHierarchyTest {

    private static Persona owner;

    @BeforeEach
    void setUp() {
        ir.ac.kntu.util.PersonaRepository.clearPersonas();
        PersonaService.reset();
        owner = PersonaService.getProfile("admin@system.local");
    }

    @Test
    void bootstrapCreatesAnOwnerAdmin() {
        assertNotNull(owner);
        assertTrue(owner.isOwner());
        assertEquals(UserRole.ADMIN, owner.getRole());
    }

    @Test
    void ownerCanCreateAdmin() {
        AdminManagementService.createAdmin(owner, "a1@test.com", "Pass123!");
        Persona a1 = PersonaService.getProfile("a1@test.com");

        assertNotNull(a1);
        assertEquals(UserRole.ADMIN, a1.getRole());
        assertFalse(a1.isOwner());
        assertEquals(owner.getEmail(), a1.getCreatedBy());
    }

    @Test
    void onlyAnAdminCanCreateAdmin() {
        Persona guest = PersonaService.registerPersona("guest@test.com", "pass");

        assertThrows(AuthorizationException.class,
                () -> AdminManagementService.createAdmin(guest, "a1@test.com", "Pass123!"));
    }

    @Test
    void adminCanManageOnlyAdminsItPersonallyCreated() {
        AdminManagementService.createAdmin(owner, "a1@test.com", "Pass123!");
        AdminManagementService.createAdmin(owner, "a2@test.com", "Pass123!");
        Persona a1 = PersonaService.getProfile("a1@test.com");

        AdminManagementService.createAdmin(a1, "a1child@test.com", "Pass123!");

        // a1 may manage its own child...
        AdminManagementService.deleteAdmin(a1, "a1child@test.com");
        assertNull(PersonaService.getProfile("a1child@test.com"));

        // ...but not a sibling admin created by someone else (here, the owner).
        assertThrows(AuthorizationException.class,
                () -> AdminManagementService.deleteAdmin(a1, "a2@test.com"));
    }

    @Test
    void ownerCanManageAnyAdminRegardlessOfCreator() {
        AdminManagementService.createAdmin(owner, "a1@test.com", "Pass123!");
        Persona a1 = PersonaService.getProfile("a1@test.com");
        AdminManagementService.createAdmin(a1, "a2@test.com", "Pass123!");

        AdminManagementService.deleteAdmin(owner, "a2@test.com");
        AdminManagementService.deleteAdmin(owner, "a1@test.com");

        assertNull(PersonaService.getProfile("a1@test.com"));
        assertNull(PersonaService.getProfile("a2@test.com"));
    }

    @Test
    void nobodyCanManageTheOwner() {
        AdminManagementService.createAdmin(owner, "a1@test.com", "Pass123!");
        Persona a1 = PersonaService.getProfile("a1@test.com");

        assertThrows(AuthorizationException.class,
                () -> AdminManagementService.deleteAdmin(a1, owner.getEmail()));
        assertThrows(AuthorizationException.class,
                () -> AdminManagementService.deleteAdmin(owner, owner.getEmail()));
    }

    @Test
    void deletingAnUnknownAdminThrows() {
        assertThrows(UserNotFoundException.class,
                () -> AdminManagementService.deleteAdmin(owner, "ghost@test.com"));
    }

    @Test
    void ownerCanPromoteAndDemoteAnyone() {
        Persona student = PersonaService.registerPersona("student@test.com", "pass");
        student.updateRole(UserRole.STUDENT);

        AdminManagementService.promoteAdmin(owner, "student@test.com", UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, PersonaService.getProfile("student@test.com").getRole());

        AdminManagementService.demoteAdmin(owner, "student@test.com", UserRole.STUDENT);
        assertEquals(UserRole.STUDENT, PersonaService.getProfile("student@test.com").getRole());
    }

    @Test
    void adminCannotDemoteAnAdminItDidNotCreate() {
        AdminManagementService.createAdmin(owner, "a1@test.com", "Pass123!");
        AdminManagementService.createAdmin(owner, "a2@test.com", "Pass123!");
        Persona a1 = PersonaService.getProfile("a1@test.com");

        assertThrows(AuthorizationException.class,
                () -> AdminManagementService.demoteAdmin(a1, "a2@test.com", UserRole.STUDENT));
    }

    @Test
    void ownerCanResetAnAdminsPassword() {
        AdminManagementService.createAdmin(owner, "a1@test.com", "Pass123!");

        AdminManagementService.resetPassword(owner, "a1@test.com", "NewPass456!");

        assertTrue(PersonaService.validateCredentials("a1@test.com", "NewPass456!"));
    }

    @Test
    void adminCannotResetTheOwnersPassword() {
        AdminManagementService.createAdmin(owner, "a1@test.com", "Pass123!");
        Persona a1 = PersonaService.getProfile("a1@test.com");

        assertThrows(AuthorizationException.class,
                () -> AdminManagementService.resetPassword(a1, owner.getEmail(), "hacked"));
    }

    @Test
    void onlyAdminsCanAssignSupportSections() {
        Persona cc = PersonaService.registerPersona("cc1@test.com", "pass");
        cc.updateRole(UserRole.CALLCENTER);
        Persona guest = PersonaService.registerPersona("guest@test.com", "pass");

        assertThrows(AuthorizationException.class,
                () -> AdminManagementService.assignSupportSections(guest, "cc1@test.com",
                        java.util.EnumSet.of(ir.ac.kntu.support.SupportSection.TECHNICAL)));
    }

    @Test
    void assigningSectionsToNonCallCenterPersonaThrows() {
        Persona student = PersonaService.registerPersona("student@test.com", "pass");
        student.updateRole(UserRole.STUDENT);

        assertThrows(UserNotFoundException.class,
                () -> AdminManagementService.assignSupportSections(owner, "student@test.com",
                        java.util.EnumSet.of(ir.ac.kntu.support.SupportSection.TECHNICAL)));
    }
}
