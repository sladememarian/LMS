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
    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    static {
        String defaultCcUser = EnvConfig.get("DEFAULT_CALLCENTER_USERNAME", "callcenter");
        String defaultCcPass = EnvConfig.get("DEFAULT_CALLCENTER_PASSWORD", "ccpass");
        
        String defaultAdminUser = EnvConfig.get("DEFAULT_ADMIN_USERNAME", "admin");
        String defaultAdminPass = EnvConfig.get("DEFAULT_ADMIN_PASSWORD", "adminpass");

        
        PERSONA_DATABASE.add(new Persona(defaultCcUser, defaultCcPass, UserRole.CALLCENTER));
        PERSONA_DATABASE.add(new Persona(defaultAdminUser, defaultAdminPass, UserRole.ADMIN));
    }

    public static void registerPersona(String email, String password) {
        Persona persona = new Persona(email, password);
        PERSONA_DATABASE.add(persona);
        saveToEncryptedFile();
    }

    public static boolean validateCredentials(String email, String password) {
        if (PERSONA_DATABASE.isEmpty()) {
            loadFromEncryptedFile();
        }
        for (Persona persona : PERSONA_DATABASE) {
            if (persona.getEmail().equalsIgnoreCase(email) && persona.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public static Persona getProfile(String email) {
        for (Persona persona : PERSONA_DATABASE) {
            if (persona.getEmail().equalsIgnoreCase(email)) {
                return persona;
            }
        }
        return null;
    }

    private static void saveToEncryptedFile() {
        StringBuilder jsonBuilder = new StringBuilder("[\n");
        String fieldSuffix = "\",\n";
        for (int i = 0; i < PERSONA_DATABASE.size(); i++) {
            Persona persona = PERSONA_DATABASE.get(i);
            jsonBuilder.append("  {\n")
                    .append("    \"email\": \"").append(persona.getEmail()).append(fieldSuffix)
                    .append("    \"password\": \"").append(persona.getPassword()).append(fieldSuffix)
                    .append("    \"role\": \"").append(persona.getRole().name()).append(fieldSuffix)
                    .append("    \"memberId\": \"").append(persona.getMemberId()).append("\"\n")
                    .append("  }");
            if (i < PERSONA_DATABASE.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        jsonBuilder.append("]");

        byte[] keyBytes = getEncryptionKey();
        byte[] rawBytes = jsonBuilder.toString().getBytes();
        byte[] encryptedBytes = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            encryptedBytes[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(encryptedBytes);
        } catch (IOException e) {
            System.err.println("Error saving persona data: " + e.getMessage());
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
            String jsonString = new String(decryptedBytes);
            parseJsonToPersonaList(jsonString);
        } catch (IOException e) {
            System.err.println("Error loading persona data: " + e.getMessage());
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
            String email = extractValue(cleanObj, "email");
            String password = extractValue(cleanObj, "password");
            String username = extractValue(cleanObj, "username");
            String roleStr = extractValue(cleanObj, "role");
            String memberId = extractValue(cleanObj, "memberId");
            String walletBalanceStr = extractValue(cleanObj, "walletBalance");

            if (email != null && password != null) {
                UserRole role = UserRole.valueOf(roleStr != null ? roleStr : "GUEST");
                int walletBalance = walletBalanceStr != null ? Integer.parseInt(walletBalanceStr) : 0;
                PERSONA_DATABASE.add(new Persona(email, username, password, role, memberId, walletBalance));
            }
        }
    }

    private static String extractValue(String source, String key) {
        String targetKey = "\"" + key + "\": \"";
        int startIndex = source.indexOf(targetKey);
        if (startIndex == -1) {
            return null;
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
        }
    }

    public static void transferToAdmin(int taxAmount) {
        for (Persona persona : PERSONA_DATABASE) {
            if (persona.getRole() == UserRole.ADMIN) {
                persona.setWalletBalance(persona.getWalletBalance() + taxAmount);
                break;
            }
        }
        saveToEncryptedFile();
    }

    public static Persona getProfileByUsername(String username) {
        if (username == null) {
            return null;
        }
        for (Persona profile : PERSONA_DATABASE) {
            if (username.equalsIgnoreCase(profile.getUsername())) {
                return profile;
            }
        }
        return null;
    }
}
