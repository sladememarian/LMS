package ir.ac.kntu.persona;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.util.EnvConfig;

public class PersonaService {

    private static final String FILE_PATH = "persona_secure.json";
    private static final List<Persona> PERSONA_DATABASE = new ArrayList<>();
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ROLE = "role";
    private static final String KEY_MEMBER_ID = "memberId";
    private static final String KEY_WALLET = "walletBalance";
    private static final String KEY_FIRST = "firstName";
    private static final String KEY_LAST = "lastName";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_THEME = "theme";
    private static final String KEY_BORROWED = "borrowed";
    private static final String BORROW_SEPARATOR = "\\|";

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    static {
        loadFromEncryptedFile();

        String defaultCcUser = EnvConfig.get("DEFAULT_CALLCENTER_USERNAME", "callcenter");
        String defaultCcPass = EnvConfig.get("DEFAULT_CALLCENTER_PASSWORD", "ccpass");
        String defaultAdminUser = EnvConfig.get("DEFAULT_ADMIN_USERNAME", "admin");
        String defaultAdminPass = EnvConfig.get("DEFAULT_ADMIN_PASSWORD", "adminpass");

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
            PERSONA_DATABASE.add(new Persona(defaultAdminUser, defaultAdminPass, UserRole.ADMIN));
        }
        if (!hasCallcenter) {
            PERSONA_DATABASE.add(new Persona(defaultCcUser, defaultCcPass, UserRole.CALLCENTER));
        }
    }

    public static Persona registerPersona(String email, String password) {
        Persona persona = new Persona(email, password);
        PERSONA_DATABASE.add(persona);
        saveToEncryptedFile();
        return persona;
    }

    public static boolean validateCredentials(String email, String password) {
        loadFromEncryptedFile();
        for (Persona persona : PERSONA_DATABASE) {
            if (persona.getEmail() != null && persona.getEmail().equalsIgnoreCase(email)
                    && persona.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public static Persona getProfile(String email) {
        for (Persona persona : PERSONA_DATABASE) {
            if (persona.getEmail() != null && persona.getEmail().equalsIgnoreCase(email)) {
                return persona;
            }
        }
        return null;
    }

    public static void updateProfile(String email, String firstName, String lastName, String phoneNumber) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.setFirstName(firstName);
            persona.setLastName(lastName);
            persona.setPhoneNumber(phoneNumber);
            saveToEncryptedFile();
        }
    }

    public static boolean updatePassword(String email, String newPassword) {
        Persona persona = getProfile(email);
        if (persona == null) {
            return false;
        }
        persona.setPassword(newPassword);
        saveToEncryptedFile();
        return true;
    }

    public static void updateTheme(String email, String theme) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.setTheme(theme);
            saveToEncryptedFile();
        }
    }

    private static String field(String key, String value, boolean last) {
        String safe = value == null ? "" : value;
        return "    \"" + key + "\": \"" + safe + "\"" + (last ? "\n" : ",\n");
    }

    private static void appendPersona(StringBuilder builder, Persona persona) {
        builder.append("  {\n")
                .append(field(KEY_EMAIL, persona.getEmail(), false))
                .append(field(KEY_USERNAME, persona.getUsername(), false))
                .append(field(KEY_PASSWORD, persona.getPassword(), false))
                .append(field(KEY_ROLE, persona.getRole().name(), false))
                .append(field(KEY_MEMBER_ID, persona.getMemberId(), false))
                .append(field(KEY_WALLET, String.valueOf(persona.getWalletBalance()), false))
                .append(field(KEY_FIRST, persona.getFirstName(), false))
                .append(field(KEY_LAST, persona.getLastName(), false))
                .append(field(KEY_PHONE, persona.getPhoneNumber(), false))
                .append(field(KEY_THEME, persona.getTheme(), false))
                .append(field(KEY_BORROWED, String.join("|", persona.getBorrowedItemIds()), true))
                .append("  }");
    }

    private static void saveToEncryptedFile() {
        StringBuilder builder = new StringBuilder("[\n");
        for (int i = 0; i < PERSONA_DATABASE.size(); i++) {
            appendPersona(builder, PERSONA_DATABASE.get(i));
            if (i < PERSONA_DATABASE.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("]");

        byte[] keyBytes = getEncryptionKey();
        byte[] rawBytes = builder.toString().getBytes();
        byte[] encryptedBytes = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            encryptedBytes[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(encryptedBytes);
        } catch (IOException ex) {
            System.err.println("Error saving persona data: " + ex.getMessage());
        }
    }

    private static void loadFromEncryptedFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] encryptedBytes = fis.readAllBytes();
            byte[] keyBytes = getEncryptionKey();
            byte[] decryptedBytes = new byte[encryptedBytes.length];
            for (int i = 0; i < encryptedBytes.length; i++) {
                decryptedBytes[i] = (byte) (encryptedBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            parseJsonToPersonaList(new String(decryptedBytes));
        } catch (IOException ex) {
            System.err.println("Error loading persona data: " + ex.getMessage());
        }
    }

    private static Persona buildPersona(String obj) {
        String email = emptyToNull(extractValue(obj, KEY_EMAIL));
        String username = emptyToNull(extractValue(obj, KEY_USERNAME));
        String password = extractValue(obj, KEY_PASSWORD);
        String roleStr = extractValue(obj, KEY_ROLE);
        String memberId = extractValue(obj, KEY_MEMBER_ID);
        String walletStr = extractValue(obj, KEY_WALLET);
        UserRole role = roleStr.isEmpty() ? UserRole.GUEST : UserRole.valueOf(roleStr);
        int wallet = walletStr.isEmpty() ? 0 : Integer.parseInt(walletStr);
        Persona persona = new Persona(email, username, password, role, memberId, wallet);
        persona.setFirstName(emptyToNull(extractValue(obj, KEY_FIRST)));
        persona.setLastName(emptyToNull(extractValue(obj, KEY_LAST)));
        persona.setPhoneNumber(emptyToNull(extractValue(obj, KEY_PHONE)));
        String theme = extractValue(obj, KEY_THEME);
        persona.setTheme(theme.isEmpty() ? "LIGHT" : theme);
        restoreBorrowedItems(persona, extractValue(obj, KEY_BORROWED));
        return persona;
    }

    private static void restoreBorrowedItems(Persona persona, String rawBorrowed) {
        if (rawBorrowed == null || rawBorrowed.isEmpty()) {
            return;
        }
        for (String itemId : rawBorrowed.split(BORROW_SEPARATOR)) {
            persona.addBorrowedItem(itemId.trim());
        }
    }

    private static void parseJsonToPersonaList(String jsonString) {
        PERSONA_DATABASE.clear();
        String clean = jsonString.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        String[] objects = clean.split("\\},");
        for (String obj : objects) {
            String cleanObj = obj.replace("{", "").replace("}", "").trim();
            if (!cleanObj.isEmpty()) {
                PERSONA_DATABASE.add(buildPersona(cleanObj));
            }
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String extractValue(String source, String key) {
        String targetKey = "\"" + key + "\": \"";
        int startIndex = source.indexOf(targetKey);
        if (startIndex == -1) {
            return "";
        }
        startIndex += targetKey.length();
        int endIndex = source.indexOf("\"", startIndex);
        return source.substring(startIndex, endIndex);
    }

    public static int getWalletBalance(String email) {
        Persona persona = getProfile(email);
        if (persona != null) {
            return persona.getWalletBalance();
        }
        throw new IllegalArgumentException("Persona not found for email: " + email);
    }

    public static void updateWalletBalance(String email, int amount) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.setWalletBalance(persona.getWalletBalance() + amount);
            saveToEncryptedFile();
            syncCurrentUserWallet(email, persona.getWalletBalance());
        }
    }

    private static void syncCurrentUserWallet(String email, int newBalance) {
        Persona current = Persona.getCurrentUser();
        if (email != null && current != null && email.equalsIgnoreCase(current.getEmail())) {
            current.setWalletBalance(newBalance);
        }
    }

    public static void transferToAdmin(int taxAmount) {
        for (Persona persona : PERSONA_DATABASE) {
            if (persona.getRole() == UserRole.ADMIN) {
                persona.setWalletBalance(persona.getWalletBalance() + taxAmount);
                syncCurrentUserWallet(persona.getEmail(), persona.getWalletBalance());
                break;
            }
        }
        saveToEncryptedFile();
    }

    public static void recordBorrow(String email, String itemId) {
        Persona persona = getProfile(email);
        if (persona != null) {
            persona.addBorrowedItem(itemId);
            saveToEncryptedFile();
            Persona current = Persona.getCurrentUser();
            if (current != null && email.equalsIgnoreCase(current.getEmail())) {
                current.addBorrowedItem(itemId);
            }
        }
    }

    public static boolean promoteRole(String email, UserRole newRole) {
        loadFromEncryptedFile();
        Persona persona = getProfile(email);
        if (persona == null) {
            return false;
        }
        persona.updateRole(newRole);
        saveToEncryptedFile();
        return true;
    }

    public static boolean recordReturn(String email, String itemId) {
        Persona persona = getProfile(email);
        if (persona == null) {
            return false;
        }
        boolean removed = persona.removeBorrowedItem(itemId);
        if (removed) {
            saveToEncryptedFile();
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
        loadFromEncryptedFile();
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
        loadFromEncryptedFile();
        for (Persona profile : PERSONA_DATABASE) {
            if (username.equalsIgnoreCase(profile.getUsername())) {
                return profile;
            }
        }
        return null;
    }
}
