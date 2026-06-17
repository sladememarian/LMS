package ir.ac.kntu.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.util.EnvConfig;

public class SupportService {

    private static final List<SupportTicket> TICKETS = new ArrayList<>();
    private static final String FILE_PATH = "support_tickets.json";

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    public static boolean validateCallCenterLogin(String username, String password) {
        Persona profile = PersonaService.getProfile(username);

        if (profile != null && profile.getPassword().equals(password) && profile.getRole() == ir.ac.kntu.persona.UserRole.CALLCENTER) {
            Persona.setCurrentUser(profile);
            return true;
        }
        return false;
    }

    public static void createTicket(String userId, String category, String title, String description) {
        if (TICKETS.isEmpty()) {
            loadTicketsFromEncryptedFile();
        }
        String priority = "LOW";
        if ("Technical".equalsIgnoreCase(category)) {
            priority = "HIGH";
        }
        String upperTitle = title.toUpperCase();
        if (upperTitle.contains("URGENT") || upperTitle.contains("CRASH") || upperTitle.contains("BLOCK")) {
            priority = "CRITICAL";
        }

        String id = "TCK-" + ((int) (Math.random() * 900_000) + 100_000);
        SupportTicket ticket = new SupportTicket(id, userId, title, description);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus("OPEN");
        TICKETS.add(ticket);
        Collections.sort(TICKETS);
        saveTicketsToEncryptedFile();
    }

    public static List<SupportTicket> getAllTickets() {
        if (TICKETS.isEmpty()) {
            loadTicketsFromEncryptedFile();
        }
        return new ArrayList<>(TICKETS);
    }

    public static boolean submitLibraryItemPlaceholder(String type, String title, String author) {
        // In a real implementation, this would save to a database or file
        // System.out.println("Received library item placeholder: " + type + " - " + title + " by " + author);
        // comming soon in next commits :)
        return type != null && title != null && author != null;
    }

    private static void saveTicketsToEncryptedFile() {
        StringBuilder sb = new StringBuilder("[\n");
        String suffix = "\",\n";
        for (int i = 0; i < TICKETS.size(); i++) {
            SupportTicket ticket = TICKETS.get(i);
            sb.append("  {\n")
                    .append("    \"id\": \"").append(ticket.getTicketId()).append(suffix)
                    .append("    \"uid\": \"").append(ticket.getUserId()).append(suffix)
                    .append("    \"cat\": \"").append(ticket.getCategory()).append(suffix)
                    .append("    \"ttl\": \"").append(ticket.getTitle()).append(suffix)
                    .append("    \"desc\": \"").append(ticket.getDescription()).append(suffix)
                    .append("    \"stat\": \"").append(ticket.getStatus()).append(suffix)
                    .append("    \"pri\": \"").append(ticket.getPriority()).append("\"\n")
                    .append("  }");
            if (i < TICKETS.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        byte[] keyBytes = getEncryptionKey();
        byte[] rawBytes = sb.toString().getBytes();
        byte[] enc = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            enc[i] = (byte) (rawBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(enc);
        } catch (IOException e) {
            System.err.println("Error saving support data: " + e.getMessage());
        }
    }

    private static void loadTicketsFromEncryptedFile() {
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
            parseSupportJson(new String(dec));
        } catch (IOException e) {
            System.err.println("Error loading support data: " + e.getMessage());
        }
    }

    private static void parseSupportJson(String raw) {
        TICKETS.clear();
        String clean = raw.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        String[] blocks = clean.split("\\},");
        for (String block : blocks) {
            String obj = block.replace("{", "").replace("}", "").trim();
            String id = extract(obj, "id");
            String uid = extract(obj, "uid");
            String cat = extract(obj, "cat");
            String ttl = extract(obj, "ttl");
            String desc = extract(obj, "desc");
            String stat = extract(obj, "stat");
            String pri = extract(obj, "pri");

            if (id != null && uid != null) {
                SupportTicket ticket = new SupportTicket(id, uid, ttl, desc);
                ticket.setCategory(cat);
                ticket.setPriority(pri != null ? pri : "LOW");
                ticket.setStatus(stat);
                TICKETS.add(ticket);
            }
        }
        Collections.sort(TICKETS);
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

    public static void handleCallCenterStockUpdate(String itemId, int quantity) {
        if (Persona.getCurrentUser() != null && Persona.getCurrentUser().getRole() == UserRole.CALLCENTER) {
            LibraryService.updateItemQuantityFromCallCenter(itemId, quantity);
            System.out.println("🔄 Support Bridge: Verified agent request routed to inventory module.");
        } else {
            System.out.println("❌ Support Bridge Error: Action denied. Unauthorized security clearance scope.");
        }
    }
}
