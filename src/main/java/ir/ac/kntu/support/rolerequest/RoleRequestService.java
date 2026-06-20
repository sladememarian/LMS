package ir.ac.kntu.support.rolerequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.EnvConfig;

public class RoleRequestService {
    // processing role requests like a bureaucratic machine
    private static final List<RoleRequest> REQUESTS = new ArrayList<>();
    private static final String SUBJECT = "Role Request Update";
    private static final String FILE_PATH = "role_requests.json";
    private static final String KEY_ID = "id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_MESSAGE = "msg";
    private static final String KEY_STATUS = "status";

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'").replace("\n", " ").replace("\r", " ");
    }

    public static RoleRequest submit(Persona requester, String requestedRole, String message) {
        loadFromEncryptedFile();
        String id = "RR-" + ((int) (Math.random() * 900_000) + 100_000);
        RoleRequest request = new RoleRequest(id, requester.getEmail(), requestedRole, sanitize(message));
        REQUESTS.add(request);
        saveToEncryptedFile();
        return request;
    }

    public static List<RoleRequest> getPending() {
        loadFromEncryptedFile();
        List<RoleRequest> pending = new ArrayList<>();
        for (RoleRequest request : REQUESTS) {
            if (RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
                pending.add(request);
            }
        }
        return pending;
    }

    public static List<RoleRequest> getAll() {
        loadFromEncryptedFile();
        return new ArrayList<>(REQUESTS);
    }

    public static boolean approve(String requestId) {
        loadFromEncryptedFile();
        RoleRequest request = find(requestId);
        if (request == null || !RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        request.setStatus(RoleRequest.STATUS_APPROVED);
        saveToEncryptedFile();
        PersonaService.promoteRole(request.getRequesterEmail(), UserRole.valueOf(request.getRequestedRole()));
        NotificationService.notifyAddress(request.getRequesterEmail(), SUBJECT,
                "Approved: you are now " + request.getRequestedRole());
        return true;
    }

    public static boolean reject(String requestId) {
        loadFromEncryptedFile();
        RoleRequest request = find(requestId);
        if (request == null || !RoleRequest.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        request.setStatus(RoleRequest.STATUS_REJECTED);
        saveToEncryptedFile();
        NotificationService.notifyAddress(request.getRequesterEmail(), SUBJECT,
                "Rejected: your " + request.getRequestedRole() + " request was declined");
        return true;
    }

    private static RoleRequest find(String requestId) {
        for (RoleRequest request : REQUESTS) {
            if (request.getRequestId().equals(requestId)) {
                return request;
            }
        }
        return null;
    }

    private static void appendRequest(StringBuilder builder, RoleRequest request) {
        String suffix = "\",\n";
        builder.append("  {\n")
                .append("    \"id\": \"").append(request.getRequestId()).append(suffix)
                .append("    \"email\": \"").append(sanitize(request.getRequesterEmail())).append(suffix)
                .append("    \"role\": \"").append(request.getRequestedRole()).append(suffix)
                .append("    \"msg\": \"").append(sanitize(request.getMessage())).append(suffix)
                .append("    \"status\": \"").append(request.getStatus()).append("\"\n")
                .append("  }");
    }

    private static void saveToEncryptedFile() {
        StringBuilder builder = new StringBuilder("[\n");
        for (int i = 0; i < REQUESTS.size(); i++) {
            appendRequest(builder, REQUESTS.get(i));
            if (i < REQUESTS.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("]");
        byte[] keyBytes = getEncryptionKey();
        byte[] rawBytes = builder.toString().getBytes();
        byte[] enc = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            enc[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(enc);
        } catch (IOException ex) {
            System.err.println("Error saving role requests: " + ex.getMessage());
        }
    }

    private static void loadFromEncryptedFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] enc = fis.readAllBytes();
            byte[] keyBytes = getEncryptionKey();
            byte[] dec = new byte[enc.length];
            for (int i = 0; i < enc.length; i++) {
                dec[i] = (byte) (enc[i] ^ keyBytes[i % keyBytes.length]);
            }
            parseJson(new String(dec));
        } catch (IOException ex) {
            System.err.println("Error loading role requests: " + ex.getMessage());
        }
    }

    private static void parseJson(String raw) {
        REQUESTS.clear();
        String clean = raw.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        for (String block : clean.split("\\},")) {
            String obj = block.replace("{", "").replace("}", "").trim();
            String id = extract(obj, KEY_ID);
            if (id != null) {
                RoleRequest request = new RoleRequest(id, extract(obj, KEY_EMAIL),
                        extract(obj, KEY_ROLE), extract(obj, KEY_MESSAGE));
                request.setStatus(extract(obj, KEY_STATUS));
                REQUESTS.add(request);
            }
        }
    }

    private static String extract(String src, String key) {
        String token = "\"" + key + "\": \"";
        int start = src.indexOf(token);
        if (start == -1) {
            return null;
        }
        start += token.length();
        return src.substring(start, src.indexOf("\"", start));
    }
}