package ir.ac.kntu.support.inbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import ir.ac.kntu.util.EnvConfig;

import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.TicketPrinter;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.support.rolerequest.RoleRequest;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

/**
 * Admin inbox: the master operator console of Support. Approves/rejects role
 * requests, monitors tickets and CallCenter activity, and inspects the store.
 */
public class AdminInbox {

    private static final String PROMPT_REQUEST = "Request ID: ";

    public static void open(Scanner scanner, Persona admin) {
        boolean active = true;
        while (active) {
            printMenu();
            String choice = ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
            active = handle(choice, scanner, admin);
        }
    }

    private static void printMenu() {
        ConsoleMenu.banner("ADMIN INBOX");
        ConsoleMenu.option("1", "View Role Requests");
        ConsoleMenu.option("2", "Approve Request");
        ConsoleMenu.option("3", "Reject Request");
        ConsoleMenu.option("4", "View User Tickets");
        ConsoleMenu.option("5", "View CallCenter Activity");
        ConsoleMenu.option("6", "View Notifications");
        ConsoleMenu.option("7", "View Encrypted Database");
        ConsoleMenu.option("8", "Debug Tools");
        ConsoleMenu.option("9", "Advance Simulated Day (Time God)");
        ConsoleMenu.back();
    }

    private static boolean handle(String choice, Scanner scanner, Persona admin) {
        switch (choice) {
            case "1":
                printRequests();
                return true;
            case "2":
                review(scanner, true);
                return true;
            case "3":
                review(scanner, false);
                return true;
            case "4":
                TicketPrinter.printTickets(SupportService.getAllTickets());
                return true;
            case "0":
                return false;
            default:
                return handleAdvanced(choice, scanner, admin);
        }
    }

    private static boolean handleAdvanced(String choice, Scanner scanner, Persona admin) {
        switch (choice) {
            case "5":
                printActivity();
                return true;
            case "6":
                NotificationService.showNotifications(scanner, admin);
                return true;
            case "7":
                inspectDatabase();
                return true;
            case "8":
                System.out.println("  Pending role requests: " + RoleRequestService.getPending().size());
                return true;
            case "9":
                advanceSimulatedDay();
                return true;
            default:
                ConsoleColor.printError("Invalid entry.");
                return true;
        }
    }

    private static void advanceSimulatedDay() {
        int newDay = SimulationClock.advanceDay();
        List<String> charges = LoanService.accrueOverdueDebts(newDay);
        System.out.println(ConsoleColor.BOLD + "Simulated day advanced to day " + newDay
                + " (" + SimulationClock.formatCurrentDate() + ")" + ConsoleColor.RESET);
        if (charges.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no overdue fines accrued today)"));
            return;
        }
        System.out.println("  Overdue fines injected into member debts:");
        for (String charge : charges) {
            System.out.println(ConsoleColor.gray("   - " + charge));
        }
    }

    private static void printRequests() {
        List<RoleRequest> pending = RoleRequestService.getPending();
        if (pending.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no pending requests)"));
            return;
        }
        for (RoleRequest request : pending) {
            System.out.println(ConsoleColor.CYAN + "  " + request.getRequestId() + ConsoleColor.RESET
                    + " | " + request.getRequesterEmail() + " -> " + request.getRequestedRole()
                    + ConsoleColor.gray(" : " + request.getMessage()));
        }
    }

    private static void review(Scanner scanner, boolean approve) {
        String requestId = ConsoleMenu.readLine(scanner, PROMPT_REQUEST);
        boolean done = approve
                ? RoleRequestService.approve(requestId) : RoleRequestService.reject(requestId);
        if (done) {
            ConsoleColor.printSuccess("Request " + requestId + (approve ? " approved." : " rejected."));
        } else {
            ConsoleColor.printError("Request not found or already processed.");
        }
    }

    private static void printActivity() {
        int closed = 0;
        for (SupportTicket ticket : SupportService.getAllTickets()) {
            if ("CLOSED".equals(ticket.getStatus())) {
                closed++;
            }
        }
        System.out.println("  Total tickets: " + SupportService.getAllTickets().size());
        System.out.println("  Closed tickets: " + closed);
    }

    private static String decryptFile(String path) throws IOException {
        File file = new File(path);
        System.out.println("  Size: " + file.length() + " bytes");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] encryptedData = fis.readAllBytes();
            byte[] keyBytes = EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackKey").getBytes();
            byte[] decrypted = new byte[encryptedData.length];
            for (int i = 0; i < encryptedData.length; i++) {
                decrypted[i] = (byte) (encryptedData[i] ^ keyBytes[i % keyBytes.length]);
            }
            return new String(decrypted);
        }
    }

    private static void writeMergedExport(Map<String, String> merged) {
        String exportPath = "merged_decrypted_export.json";
        try (FileOutputStream fos = new FileOutputStream(exportPath)) {
            StringBuilder sb = new StringBuilder("{\n");
            int idx = 0;
            for (Map.Entry<String, String> entry : merged.entrySet()) {
                sb.append("  \"").append(entry.getKey()).append("\": ").append(entry.getValue());
                if (idx < merged.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
                idx++;
            }
            sb.append("}\n");
            fos.write(sb.toString().getBytes());
            System.out.println(ConsoleColor.BOLD + "Merged export written to: " + exportPath + ConsoleColor.RESET);
            ConsoleColor.printError("WARNING: This file contains ALL decrypted data in plain text.");
            ConsoleColor.printError("DELETE " + exportPath + " IMMEDIATELY after closing the program!");
        } catch (IOException ex) {
            ConsoleColor.printError("Failed to write merged export: " + ex.getMessage());
        }
    }

    private static void inspectDatabase() {
        String[] encryptedFiles = {
            "support_tickets.json",
            "finance_secure.json",
            "persona_secure.json",
            "role_requests.json",
            "loans_secure.json",
            "clock_secure.json",
            "mail.enc",
            "library.enc"
        };
        Map<String, String> merged = new LinkedHashMap<>();
        for (String path : encryptedFiles) {
            System.out.println(ConsoleColor.BOLD + "--- " + path + " ---" + ConsoleColor.RESET);
            File file = new File(path);
            if (!file.exists()) {
                System.out.println(ConsoleColor.gray("  (not yet created)"));
                merged.put(path, "null");
                System.out.println();
                continue;
            }
            try {
                String content = decryptFile(path);
                System.out.println(content);
                merged.put(path, content);
            } catch (IOException ex) {
                ConsoleColor.printError("  Failed to read: " + ex.getMessage());
            }
            System.out.println();
        }
        writeMergedExport(merged);
    }
}
