package ir.ac.kntu.persona;

import ir.ac.kntu.exception.DuplicateEmailException;
import ir.ac.kntu.exception.UserNotFoundException;
import ir.ac.kntu.util.DatabaseAccess;
import ir.ac.kntu.util.EnvConfig;
import java.util.ArrayList;
import java.util.List;

public class PersonaService {

    private static final List<Persona> PERSONA_DATABASE = new ArrayList<>();

    static {
        PERSONA_DATABASE.addAll(DatabaseAccess.getAllPersonas());

        String defaultAdminPass = EnvConfig.get(
            "DEFAULT_ADMIN_PASSWORD",
            "adminpass"
        );
        String defaultCcPass = EnvConfig.get(
            "DEFAULT_CALLCENTER_PASSWORD",
            "ccpass"
        );

        boolean hasAdmin = false;
        boolean hasCallcenter = false;
        for (Persona p : PERSONA_DATABASE) {
            if (p.getRole() == UserRole.ADMIN) {
                hasAdmin = true;
            }
            if (p.getRole() == UserRole.CALLCENTER) {
                hasCallcenter = true;
            }
        }
        if (!hasAdmin) {
            Persona admin = new Persona("admin@system.local", defaultAdminPass);
            admin.updateRole(UserRole.ADMIN);
            PERSONA_DATABASE.add(admin);
            DatabaseAccess.insertPersona(admin);
        }
        if (!hasCallcenter) {
            Persona cc = new Persona("callcenter@system.local", defaultCcPass);
            cc.updateRole(UserRole.CALLCENTER);
            PERSONA_DATABASE.add(cc);
            DatabaseAccess.insertPersona(cc);
        }
    }

    public static Persona registerPersona(String email, String password) {
        if (getProfile(email) != null) {
            throw new DuplicateEmailException(
                "An account with email " + email + " already exists."
            );
        }
        Persona persona = new Persona(email, password);
        PERSONA_DATABASE.add(persona);
        DatabaseAccess.insertPersona(persona);
        return persona;
    }

    public static boolean validateCredentials(String email, String password) {
        PERSONA_DATABASE.clear();
        PERSONA_DATABASE.addAll(DatabaseAccess.getAllPersonas());
        for (Persona persona : PERSONA_DATABASE) {
            if (
                persona.getEmail() != null &&
                persona.getEmail().equalsIgnoreCase(email) &&
                persona.getPassword().equals(password)
            ) {
                return true;
            }
        }
        return false;
    }

    public static Persona getProfile(String email) {
        for (Persona persona : PERSONA_DATABASE) {
            if (
                persona.getEmail() != null &&
                persona.getEmail().equalsIgnoreCase(email)
            ) {
                return persona;
            }
        }
        return null;
    }

    public static void updateProfile(
        String email,
        String firstName,
        String lastName,
        String phoneNumber
    ) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.setFirstName(firstName);
            persona.setLastName(lastName);
            persona.setPhoneNumber(phoneNumber);
            DatabaseAccess.insertPersona(persona);
        }
    }

    public static boolean updatePassword(String email, String newPassword) {
        Persona persona = getProfile(email);
        if (persona == null) {
            return false;
        }
        persona.setPassword(newPassword);
        DatabaseAccess.insertPersona(persona);
        return true;
    }

    public static void updateTheme(String email, String theme) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.setTheme(theme);
            DatabaseAccess.insertPersona(persona);
        }
    }

    public static int getWalletBalance(String email) {
        Persona persona = getProfile(email);
        if (persona != null) {
            return persona.getWalletBalance();
        }
        throw new UserNotFoundException(
            "Persona not found for email: " + email
        );
    }

    public static void updateWalletBalance(String email, int amount) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.setWalletBalance(persona.getWalletBalance() + amount);
            DatabaseAccess.insertPersona(persona);
            syncCurrentUserWallet(email, persona.getWalletBalance());
        }
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
                DatabaseAccess.insertPersona(persona);
                break;
            }
        }
    }

    public static void recordBorrow(String email, String itemId) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.addBorrowedItem(itemId);
            DatabaseAccess.saveBorrowedItems(persona);
            DatabaseAccess.insertPersona(persona);
            Persona current = Persona.getCurrentUser();
            if (current != null && email.equalsIgnoreCase(current.getEmail())) {
                current.addBorrowedItem(itemId);
            }
        }
    }

    public static boolean promoteRole(String email, UserRole newRole) {
        PERSONA_DATABASE.clear();
        PERSONA_DATABASE.addAll(DatabaseAccess.getAllPersonas());
        Persona persona = getProfile(email);
        if (persona == null) {
            return false;
        }
        persona.updateRole(newRole);
        DatabaseAccess.insertPersona(persona);
        return true;
    }

    public static boolean recordReturn(String email, String itemId) {
        Persona persona = getProfile(email);
        if (persona == null) {
            return false;
        }
        boolean removed = persona.removeBorrowedItem(itemId);
        if (removed) {
            DatabaseAccess.saveBorrowedItems(persona);
            DatabaseAccess.insertPersona(persona);
            Persona current = Persona.getCurrentUser();
            if (current != null && email.equalsIgnoreCase(current.getEmail())) {
                current.removeBorrowedItem(itemId);
            }
        }
        return removed;
    }

    public static Persona getProfileByMemberId(String memberId) {
        if (memberId == null) {
            return null;
        }
        PERSONA_DATABASE.clear();
        PERSONA_DATABASE.addAll(DatabaseAccess.getAllPersonas());
        for (Persona profile : PERSONA_DATABASE) {
            if (memberId.equals(profile.getMemberId())) {
                return profile;
            }
        }
        return null;
    }

    public static Persona getProfileByUsername(String username) {
        if (username == null) {
            return null;
        }
        PERSONA_DATABASE.clear();
        PERSONA_DATABASE.addAll(DatabaseAccess.getAllPersonas());
        for (Persona profile : PERSONA_DATABASE) {
            if (username.equalsIgnoreCase(profile.getUsername())) {
                return profile;
            }
        }
        return null;
    }
}
