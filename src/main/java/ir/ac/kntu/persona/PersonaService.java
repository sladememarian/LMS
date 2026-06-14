package ir.ac.kntu.persona;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PersonaService {

    private static final String FILE_PATH = "persona_secure.json";
    private static final List<Persona> PERSONA_DATABASE = new ArrayList<>();
    private static final byte XOR_KEY = 0x5A;

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

        byte[] rawBytes = jsonBuilder.toString().getBytes();
        byte[] encryptedBytes = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            encryptedBytes[i] = (byte) (rawBytes[i] ^ XOR_KEY);
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
            byte[] decryptedBytes = new byte[encryptedBytes.length];
            for (int i = 0; i < encryptedBytes.length; i++) {
                decryptedBytes[i] = (byte) (encryptedBytes[i] ^ XOR_KEY);
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
            String roleStr = extractValue(cleanObj, "role");
            String memberId = extractValue(cleanObj, "memberId");

            if (email != null && password != null) {
                UserRole role = UserRole.valueOf(roleStr != null ? roleStr : "GUEST");
                PERSONA_DATABASE.add(new Persona(email, password, role, memberId));
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
        for(Persona persona : PERSONA_DATABASE) {
            if (persona.getRole() == UserRole.ADMIN) {
                persona.setWalletBalance(persona.getWalletBalance() + taxAmount);
                break;
            }
        }
        saveToEncryptedFile();
    }

}
