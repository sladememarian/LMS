package ir.ac.kntu.persona;

import ir.ac.kntu.exception.DuplicateEmailException;
import ir.ac.kntu.exception.UserNotFoundException;
import ir.ac.kntu.util.EnvConfig;
import ir.ac.kntu.util.PersonaRepository;
import java.util.ArrayList;
import java.util.List;

public class PersonaService {

    private static final String PERSONA_NOT_FOUND = "Persona not found for email: ";
    private static final List<Persona> PERSONA_DATABASE = new ArrayList<>();

    static {
        loadPersonas();
    }

    private static void loadPersonas() {
        PERSONA_DATABASE.clear();
        PERSONA_DATABASE.addAll(PersonaRepository.getAllPersonas());

        boolean hasAdmin = anyPersonaHasRole(UserRole.ADMIN);
        boolean hasCallcenter = anyPersonaHasRole(UserRole.CALLCENTER);

        if (!hasAdmin) {
            bootstrapDefaultAdmin();
        } else if (!anyAdminIsOwner()) {
            promoteOldestAdminToOwner();
        }
        if (!hasCallcenter) {
            bootstrapDefaultCallcenter();
        }
    }

    private static boolean anyPersonaHasRole(UserRole role) {
        return PERSONA_DATABASE.stream()
                .anyMatch(p -> p.getRole() == role);
    }

    private static boolean anyAdminIsOwner() {
        return PERSONA_DATABASE.stream()
                .anyMatch(p -> p.getRole() == UserRole.ADMIN
                        && p.isOwner());
    }

    private static void bootstrapDefaultAdmin() {
        String defaultAdminPass = EnvConfig.get("DEFAULT_ADMIN_PASSWORD", "adminpass");
        Persona admin = new Persona("admin@system.local", defaultAdminPass);
        admin.updateRole(UserRole.ADMIN);
        admin.setOwner(true);
        PERSONA_DATABASE.add(admin);
        PersonaRepository.insertPersona(admin);
    }

    // Data created before the Owner/Admin hierarchy existed: the first Admin
    // on record (the one nobody "created") becomes the Owner.
    private static void promoteOldestAdminToOwner() {
        for (Persona p : PERSONA_DATABASE) {
            if (p.getRole() == UserRole.ADMIN && p.getCreatedBy() == null) {
                p.setOwner(true);
                PersonaRepository.insertPersona(p);
                break;
            }
        }
    }

    private static void bootstrapDefaultCallcenter() {
        String defaultCcPass = EnvConfig.get("DEFAULT_CALLCENTER_PASSWORD", "ccpass");
        Persona cc = new Persona("callcenter@system.local", defaultCcPass);
        cc.updateRole(UserRole.CALLCENTER);
        PERSONA_DATABASE.add(cc);
        PersonaRepository.insertPersona(cc);
    }

    public static void reset() {
        loadPersonas();
    }

    private static void ensureLoaded() {
        PERSONA_DATABASE.clear();
        PERSONA_DATABASE.addAll(PersonaRepository.getAllPersonas());
    }

    public static Persona registerPersona(String email, String password) {
        if (getProfile(email) != null) {
            throw new DuplicateEmailException(
                "An account with email " + email + " already exists."
            );
        }
        Persona persona = new Persona(email, password);
        PERSONA_DATABASE.add(persona);
        PersonaRepository.insertPersona(persona);
        return persona;
    }

    public static boolean validateCredentials(String email,
            String password) {
        ensureLoaded();
        return PERSONA_DATABASE.stream()
                .anyMatch(p -> p.getEmail() != null
                        && p.getEmail().equalsIgnoreCase(email)
                        && p.getPassword().equals(password));
    }

    public static Persona getProfile(String email) {
        return PERSONA_DATABASE.stream()
                .filter(p -> p.getEmail() != null
                        && p.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    // Looks up a profile by email, throwing if it doesn't exist. Replaces the
    // repeated "getProfile then null-check then throw" block used across this class.
    private static Persona requireProfile(String email) {
        Persona persona = getProfile(email);
        if (persona == null) {
            throw new UserNotFoundException(PERSONA_NOT_FOUND + email);
        }
        return persona;
    }

    public static void updateProfile(
        String email,
        String firstName,
        String lastName,
        String phoneNumber
    ) {
        Persona persona = requireProfile(email);
        persona.setFirstName(firstName);
        persona.setLastName(lastName);
        persona.setPhoneNumber(phoneNumber);
        PersonaRepository.insertPersona(persona);
    }

    public static void updatePassword(String email, String newPassword) {
        Persona persona = requireProfile(email);
        persona.setPassword(newPassword);
        PersonaRepository.insertPersona(persona);
    }

    public static void updateTheme(String email, String theme) {
        Persona persona = requireProfile(email);
        persona.setTheme(theme);
        PersonaRepository.insertPersona(persona);
    }

    public static int getWalletBalance(String email) {
        return requireProfile(email).getWalletBalance();
    }

    public static void updateWalletBalance(String email, int amount) {
        Persona persona = requireProfile(email);
        persona.setWalletBalance(persona.getWalletBalance() + amount);
        PersonaRepository.insertPersona(persona);
        syncCurrentUserWallet(email, persona.getWalletBalance());
    }

    private static void syncCurrentUserWallet(String email, int newBalance) {
        Persona current = Persona.getCurrentUser();
        if (email == null || current == null) {
            return;
        }
        if (email.equalsIgnoreCase(current.getEmail())) {
            current.setWalletBalance(newBalance);
        }
    }

    public static void transferToAdmin(int taxAmount) {
        for (Persona persona : PERSONA_DATABASE) {
            if (persona.getRole() == UserRole.ADMIN) {
                persona.setWalletBalance(
                    persona.getWalletBalance() + taxAmount
                );
                syncCurrentUserWallet(
                    persona.getEmail(),
                    persona.getWalletBalance()
                );
                PersonaRepository.insertPersona(persona);
                break;
            }
        }
    }

    public static void recordBorrow(String email, String itemId) {
        Persona persona = requireProfile(email);
        persona.addBorrowedItem(itemId);
        PersonaRepository.saveBorrowedItems(persona);
        PersonaRepository.insertPersona(persona);
        Persona current = Persona.getCurrentUser();
        if (current != null && email.equalsIgnoreCase(current.getEmail())) {
            current.addBorrowedItem(itemId);
        }
    }

    // Hooks so AdminManagementService can mutate the shared in-memory
    // list without duplicating it.
    public static void addPersona(Persona persona) {
        PERSONA_DATABASE.add(persona);
    }

    public static void removePersona(Persona persona) {
        PERSONA_DATABASE.remove(persona);
    }

    public static void recordReturn(String email, String itemId) {
        Persona persona = requireProfile(email);
        persona.removeBorrowedItem(itemId);
        PersonaRepository.saveBorrowedItems(persona);
        PersonaRepository.insertPersona(persona);
        Persona current = Persona.getCurrentUser();
        if (current != null && email.equalsIgnoreCase(current.getEmail())) {
            current.removeBorrowedItem(itemId);
        }
    }

    public static Persona getProfileByMemberId(String memberId) {
        if (memberId == null) {
            return null;
        }
        ensureLoaded();
        return PERSONA_DATABASE.stream()
                .filter(p -> memberId.equals(p.getMemberId()))
                .findFirst()
                .orElse(null);
    }

    public static Persona getProfileByUsername(String username) {
        if (username == null) {
            return null;
        }
        ensureLoaded();
        return PERSONA_DATABASE.stream()
                .filter(p -> username.equalsIgnoreCase(
                        p.getUsername()))
                .findFirst()
                .orElse(null);
    }

    public static void promoteRole(String email, UserRole newRole) {
        ensureLoaded();
        Persona persona = requireProfile(email);
        persona.updateRole(newRole);
        PersonaRepository.insertPersona(persona);
    }
}
