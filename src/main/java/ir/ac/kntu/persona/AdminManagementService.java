package ir.ac.kntu.persona;

import ir.ac.kntu.exception.AuthorizationException;
import ir.ac.kntu.exception.UserNotFoundException;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.util.PersonaRepository;
import ir.ac.kntu.util.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Owner/Admin hierarchy management: creating and removing Admins, promoting
// or demoting roles, resetting passwords, and assigning CallCenter support
// sections. Split out of PersonaService to keep each class focused
// on a single responsibility.
public final class AdminManagementService {
    private static final String USER_NOT_FOUND_PREFIX = "User not found: ";

    private AdminManagementService() {
    }

    public static void createAdmin(Persona creator, String email, String password) {
        if (creator.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can create new Admins.");
        }
        validateCredentials(email, password);
        Persona admin = new Persona(email, password);
        admin.updateRole(UserRole.ADMIN);
        admin.setCreatedBy(creator.getEmail());
        PersonaService.addPersona(admin);
        PersonaRepository.insertPersona(admin);
    }

    public static void createCallCenter(Persona creator, String email, String password) {
        if (creator.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can create CallCenter agents.");
        }
        validateCredentials(email, password);
        Persona agent = new Persona(email, password);
        agent.updateRole(UserRole.CALLCENTER);
        PersonaService.addPersona(agent);
        PersonaRepository.insertPersona(agent);
    }

    private static void validateCredentials(String email, String password) {
        Validator.requireValidEmail(email);
        Validator.requireValidPassword(password);
    }

    // Only the Owner, or the Admin who personally created the target admin,
    // may manage (delete/promote/demote/reset password of) that admin. Nobody
    // may manage the Owner.
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

    // Authorizes acting on `target`: if the target is an Admin, only their
    // manager (Owner or creator) may act; otherwise the actor must be an Admin.
    // Replaces the repeated if/else-if block previously copied into every method below.
    private static void requireCanManageTarget(Persona actor, Persona target, String action) {
        if (target.getRole() == UserRole.ADMIN) {
            requireCanManageAdmin(actor, target);
        } else if (actor.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can " + action + ".");
        }
    }

    private static Persona requireTarget(String email) {
        Persona target = PersonaService.getProfile(email);
        if (target == null) {
            throw new UserNotFoundException(USER_NOT_FOUND_PREFIX + email);
        }
        return target;
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
        Persona target = requireTarget(email);
        requireCanManageTarget(actor, target, "change roles");
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
        Persona target = requireTarget(email);
        requireCanManageTarget(actor, target, "reset passwords");
        target.setPassword(newPassword);
        PersonaRepository.insertPersona(target);
    }

    // Owner/Admin assigns which support sections a CallCenter agent may see.
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

    public static List<Persona> listAllUsers() {
        return new ArrayList<>(PersonaRepository.getAllPersonas());
    }

    public static List<Persona> searchUsers(String keyword) {
        if (Validator.isBlank(keyword)) {
            return new ArrayList<>();
        }
        String lower = keyword.toLowerCase();
        return PersonaRepository.getAllPersonas().stream()
                .filter(p -> matchesUser(p, lower))
                .collect(Collectors.toList());
    }

    private static boolean matchesUser(Persona persona, String lowerKeyword) {
        return containsIgnoreCase(persona.getEmail(), lowerKeyword)
                || containsIgnoreCase(persona.getFirstName(), lowerKeyword)
                || containsIgnoreCase(persona.getLastName(), lowerKeyword)
                || containsIgnoreCase(persona.getMemberId(), lowerKeyword)
                || containsIgnoreCase(persona.getRole().name(), lowerKeyword);
    }

    private static boolean containsIgnoreCase(String field, String keyword) {
        return field != null && field.toLowerCase().contains(keyword);
    }

    public static void editUserProfile(String email,
            String firstName, String lastName, String phone) {
        requireTarget(email);
        PersonaService.updateProfile(email, firstName, lastName, phone);
    }

    public static boolean toggleActive(Persona actor, String email) {
        Persona target = requireTarget(email);
        if (target.isOwner()) {
            throw new AuthorizationException("The Owner account cannot be deactivated.");
        }
        requireCanManageTarget(actor, target, "deactivate accounts");
        target.setActive(!target.isActive());
        PersonaRepository.insertPersona(target);
        return target.isActive();
    }

    public static void deleteUser(Persona actor, String email) {
        if (actor.getRole() != UserRole.ADMIN) {
            throw new AuthorizationException("Only Admins can delete users.");
        }
        Persona target = requireTarget(email);
        if (target.isOwner()) {
            throw new AuthorizationException(
                    "The Owner account cannot be deleted.");
        }
        if (target.getRole() == UserRole.ADMIN) {
            requireCanManageAdmin(actor, target);
        }
        PersonaService.removePersona(target);
        PersonaRepository.deletePersona(email);
    }
}
