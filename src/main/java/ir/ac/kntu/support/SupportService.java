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
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.util.EnvConfig;

public class SupportService {
    // managing tickets like a pro (or at least we try)
    private static final List<SupportTicket> TICKETS = new ArrayList<>();
    private static final String FILE_PATH = "support_tickets.json";

    private static byte[] getEncryptionKey() {
        return EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
    }

    public static boolean validateCallCenterLogin(String username, String password) {
        Persona profile = PersonaService.getProfileByUsername(username);

        if (profile != null && profile.getPassword().equals(password) && profile.getRole() == UserRole.CALLCENTER) {
            Persona.setCurrentUser(profile);
            return true;
        }
        return false;
    }

    public static void createTicket(String userId, String category, String title, String description) {
        loadTicketsFromEncryptedFile();
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

    public static boolean updateTicketStatus(String ticketId, String status) {
        loadTicketsFromEncryptedFile();
        for (SupportTicket ticket : TICKETS) {
            if (ticket.getTicketId().equals(ticketId)) {
                ticket.setStatus(status);
                saveTicketsToEncryptedFile();
                return true;
            }
        }
        return false;
    }

    public static boolean respondToTicket(String ticketId, String message) {
        loadTicketsFromEncryptedFile();
        for (SupportTicket ticket : TICKETS) {
            if (ticket.getTicketId().equals(ticketId)) {
                ticket.setResponse(message);
                ticket.setStatus("IN_PROGRESS");
                saveTicketsToEncryptedFile();
                notifyCreator(ticket, message);
                return true;
            }
        }
        return false;
    }

    private static void notifyCreator(SupportTicket ticket, String message) {
        Persona creator = PersonaService.getProfileByMemberId(ticket.getUserId());
        String subject = "Reply to ticket " + ticket.getTicketId();
        if (creator != null) {
            NotificationService.notify(creator, subject, message);
        } else {
            NotificationService.notifyAddress(ticket.getUserId(), subject, message);
        }
    }

    public static List<SupportTicket> getAllTickets() {
        loadTicketsFromEncryptedFile();
        return new ArrayList<>(TICKETS);
    }

    public static boolean submitLibraryItemPlaceholder(String type, String title, String author) {
        return type != null && title != null && author != null;
    }

    private static void appendTicket(StringBuilder builder, SupportTicket ticket) {
        String suffix = "\",\n";
        builder.append("  {\n")
                .append("    \"id\": \"").append(ticket.getTicketId()).append(suffix)
                .append("    \"uid\": \"").append(ticket.getUserId()).append(suffix)
                .append("    \"cat\": \"").append(ticket.getCategory()).append(suffix)
                .append("    \"ttl\": \"").append(ticket.getTitle()).append(suffix)
                .append("    \"desc\": \"").append(ticket.getDescription()).append(suffix)
                .append("    \"stat\": \"").append(ticket.getStatus()).append(suffix)
                .append("    \"pri\": \"").append(ticket.getPriority()).append(suffix)
                .append("    \"resp\": \"").append(clean(ticket.getResponse())).append("\"\n")
                .append("  }");
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'").replace("\n", " ").replace("\r", " ");
    }

    private static void saveTicketsToEncryptedFile() {
        StringBuilder builder = new StringBuilder("[\n");
        for (int i = 0; i < TICKETS.size(); i++) {
            appendTicket(builder, TICKETS.get(i));
            if (i < TICKETS.size() - 1) {
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
            System.err.println("Error saving support data: " + ex.getMessage());
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
        } catch (IOException ex) {
            System.err.println("Error loading support data: " + ex.getMessage());
        }
    }

    private static SupportTicket buildTicket(String obj) {
        SupportTicket ticket = new SupportTicket(extract(obj, "id"), extract(obj, "uid"),
                extract(obj, "ttl"), extract(obj, "desc"));
        ticket.setCategory(extract(obj, "cat"));
        String priority = extract(obj, "pri");
        ticket.setPriority(priority != null ? priority : "LOW");
        ticket.setStatus(extract(obj, "stat"));
        String resp = extract(obj, "resp");
        ticket.setResponse(resp != null ? resp : "");
        return ticket;
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
            if (extract(obj, "id") != null) {
                TICKETS.add(buildTicket(obj));
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
        Persona current = Persona.getCurrentUser();
        if (current != null && current.getRole() == UserRole.CALLCENTER) {
            LibraryService.updateItemQuantityFromCallCenter(itemId, quantity);
            System.out.println("[Support Bridge]: Verified agent request routed to inventory module.");
        } else {
            System.out.println("[Support Bridge Error]: Action denied. Unauthorized security clearance scope.");
        }
    }

    public static boolean addLibraryItemViaSupport(LibraryItem item) {
        Persona current = Persona.getCurrentUser();
        boolean allowed = current != null
                && (current.getRole() == UserRole.CALLCENTER || current.getRole() == UserRole.ADMIN);
        if (!allowed) {
            System.out.println("[Support Bridge Error]: Action denied. Unauthorized security clearance scope.");
            return false;
        }
        boolean added = LibraryService.addItem(item);
        if (added) {
            System.out.println("[Support Bridge]: New catalog item routed CallCenter -> Support -> Library.");
        }
        return added;
    }
}