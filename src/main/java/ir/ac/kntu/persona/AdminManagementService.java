package ir.ac.kntu.persona;

import ir.ac.kntu.exception.AuthorizationException;
import ir.ac.kntu.exception.UserNotFoundException;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.util.PersonaRepository;
import java.util.Set;

/**
 * Owner/Admin hierarchy management: creating and removing Admins, promoting
 * or demoting roles, resetting passwords, and assigning CallCenter support
 * sections. Split out of {@link PersonaService} to keep each class focused
 * on a single responsibility.
 */
public final class AdminManagementService {

    private AdminManagementService() {
    }

    public static void createAdmin(Persona creator, String email, String password) {
        if (creator.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can create new Admins.");
        }
        Persona admin = new Persona(email, password);
        admin.updateRole(UserRole.ADMIN);
        admin.setCreatedBy(creator.getEmail());
        PersonaService.addPersona(admin);
        PersonaRepository.insertPersona(admin);
    }

    /**
     * Only the Owner, or the Admin who personally created the target admin,
     * may manage (delete/promote/demote/reset password of) that admin. Nobody
     * may manage the Owner.
     */
    private static void requireCanManageAdmin(Persona manager, Persona target) {
        if (target.isOwner()) {
            throw new AuthorizationException("The Owner cannot be managed by another Admin.", null);
        }
        boolean isOwner = manager.isOwner();
        boolean createdTarget = manager.getEmail() != null
                && manager.getEmail().equalsIgnoreCase(target.getCreatedBy());
        if (!isOwner && !createdTarget) {
            throw new AuthorizationException("You are not authorized to manage this admin.", null);
        }
    }

    public static void deleteAdmin(Persona deleter, String emailToDelete) {
        Persona toDelete = PersonaService.getProfile(emailToDelete);
        if (toDelete == null || toDelete.getRole() != UserRole.ADMIN) {
            throw new UserNotFoundException("Admin not found: " + emailToDelete);
        }
        requireCanManageAdmin(deleter, toDelete);
        PersonaService.removePersona(toDelete);
        PersonaRepository.deletePersona(emailToDelete);
    }

    public static void promoteAdmin(Persona actor, String email, UserRole newRole) {
        Persona target = PersonaService.getProfile(email);
        if (target == null) {
            throw new UserNotFoundException("Persona not found: " + email);
        }
        if (target.getRole() == UserRole.ADMIN) {
            requireCanManageAdmin(actor, target);
        } else if (actor.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can change roles.");
        }
        target.updateRole(newRole);
        PersonaRepository.insertPersona(target);
    }

    public static void demoteAdmin(Persona actor, String email, UserRole newRole) {
        Persona target = PersonaService.getProfile(email);
        if (target == null || target.getRole() != UserRole.ADMIN) {
            throw new UserNotFoundException("Admin not found: " + email);
        }
        requireCanManageAdmin(actor, target);
        target.updateRole(newRole);
        PersonaRepository.insertPersona(target);
    }

    public static void resetPassword(Persona actor, String email, String newPassword) {
        Persona target = PersonaService.getProfile(email);
        if (target == null) {
            throw new UserNotFoundException("Persona not found: " + email);
        }
        if (target.getRole() == UserRole.ADMIN) {
            requireCanManageAdmin(actor, target);
        } else if (actor.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can reset passwords.");
        }
        target.setPassword(newPassword);
        PersonaRepository.insertPersona(target);
    }

    /**
     * Owner/Admin assigns which support sections a CallCenter agent may see.
     */
    public static void assignSupportSections(Persona actor, String agentEmail, Set<SupportSection> sections) {
        if (actor.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can assign support sections.");
        }
        Persona agent = PersonaService.getProfile(agentEmail);
        if (agent == null || agent.getRole() != UserRole.CALLCENTER) {
            throw new UserNotFoundException("CallCenter agent not found: " + agentEmail);
        }
        agent.setAssignedSupportSections(sections);
        PersonaRepository.insertPersona(agent);
    }
}
