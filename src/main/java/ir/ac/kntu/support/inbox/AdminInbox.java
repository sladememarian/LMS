package ir.ac.kntu.support.inbox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.persona.AdminManagementService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.TicketPrinter;
import ir.ac.kntu.support.notification.NotificationService;
import ir.ac.kntu.support.rolerequest.RoleRequest;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;
import ir.ac.kntu.util.LibraryItemRepository;
import ir.ac.kntu.util.LoanRepository;
import ir.ac.kntu.util.MailRepository;
import ir.ac.kntu.util.PersonaRepository;
import ir.ac.kntu.util.RoleRequestRepository;
import ir.ac.kntu.util.SupplierRepository;
import ir.ac.kntu.util.SupportTicketRepository;
import ir.ac.kntu.util.SystemSettingsService;
import ir.ac.kntu.util.TransactionRepository;

public class AdminInbox {
    private static final String PROMPT_REQUEST = "Request ID: ";
    private static final String PROMPT_EMAIL = "Email: ";
    private static final String PROMPT_CHOOSE = "Choose: ";
    private static final String INVALID_ENTRY = "Invalid entry.";

    public static void open(Scanner scanner, Persona admin) {
        boolean active = true;
        while (active) {
            printMenu();
            String choice = promptChoice(scanner);
            active = handle(choice, scanner, admin);
        }
    }

    private static String promptChoice(Scanner scanner) {
        return ConsoleMenu.readLine(scanner, ConsoleColor.YELLOW + PROMPT_CHOOSE + ConsoleColor.RESET);
    }

    private static void printMenu() {
        ConsoleMenu.banner("ADMIN INBOX");
        ConsoleMenu.option("1", "View Role Requests");
        ConsoleMenu.option("2", "Approve Request");
        ConsoleMenu.option("3", "Reject Request");
        ConsoleMenu.option("4", "View User Tickets");
        ConsoleMenu.option("5", "View CallCenter Activity");
        ConsoleMenu.option("6", "View Notifications");
        ConsoleMenu.option("7", "View Database Records");
        ConsoleMenu.option("8", "Debug Tools");
        ConsoleMenu.option("9", "Advance Simulated Day (Time God)");
        ConsoleMenu.option("10", "Manage Admins/Callcenters");
        ConsoleMenu.option("11", "User Management");
        ConsoleMenu.option("12", "Assign CallCenter Support Sections");
        ConsoleMenu.option("13", "System Settings");
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
                System.out.println("  Pending role requests: "
                        + RoleRequestService.getPending().size());
                return true;
            default:
                return handleLower(choice, scanner, admin);
        }
    }

    private static boolean handleLower(String choice, Scanner scanner, Persona admin) {
        switch (choice) {
            case "9":
                advanceSimulatedDay();
                return true;
            case "10":
                manageAdmins(scanner, admin);
                return true;
            case "11":
                AdminUserManagement.open(scanner, admin);
                return true;
            case "12":
                assignSupportSections(scanner, admin);
                return true;
            case "13":
                manageSystemSettings(scanner, admin);
                return true;
            case "0":
                return false;
            default:
                ConsoleColor.printError(INVALID_ENTRY);
                return true;
        }
    }

    private static void manageAdmins(Scanner scanner, Persona actor) {
        ConsoleMenu.banner("MANAGE USERS");
        ConsoleMenu.option("1", "Create Admin");
        ConsoleMenu.option("2", "Create CallCenter");
        ConsoleMenu.option("3", "Delete Admin");
        ConsoleMenu.option("4", "Reset Password");
        ConsoleMenu.back();
        String choice = promptChoice(scanner);
        try {
            runAdminAction(choice, scanner, actor);
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void runAdminAction(String choice, Scanner scanner, Persona actor) {
        switch (choice) {
            case "0":
                return;
            case "1":
                createAdminAction(scanner, actor);
                break;
            case "2":
                createCallCenterAction(scanner, actor);
                break;
            case "3":
                deleteAdminAction(scanner, actor);
                break;
            case "4":
                resetPasswordAction(scanner, actor);
                break;
            default:
                ConsoleColor.printError(INVALID_ENTRY);
                break;
        }
    }

    private static void createAdminAction(Scanner scanner, Persona actor) {
        String newEmail = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        String newPassword = ConsoleMenu.readLine(scanner, "Password: ");
        AdminManagementService.createAdmin(actor, newEmail, newPassword);
        ConsoleColor.printSuccess("Admin created: " + newEmail);
    }

    private static void createCallCenterAction(Scanner scanner, Persona actor) {
        String newEmail = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        String newPassword = ConsoleMenu.readLine(scanner, "Password: ");
        AdminManagementService.createCallCenter(actor, newEmail, newPassword);
        ConsoleColor.printSuccess("CallCenter agent created: " + newEmail);
    }

    private static void deleteAdminAction(Scanner scanner, Persona actor) {
        String deleteEmail = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        AdminManagementService.deleteAdmin(actor, deleteEmail);
        ConsoleColor.printSuccess("Admin deleted: " + deleteEmail);
    }

    private static void resetPasswordAction(Scanner scanner, Persona actor) {
        String targetEmail = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        String newPass = ConsoleMenu.readLine(scanner, "New password: ");
        AdminManagementService.resetPassword(actor, targetEmail, newPass);
        ConsoleColor.printSuccess("Password reset for: " + targetEmail);
    }

    private static void assignSupportSections(Scanner scanner, Persona actor) {
        String agentEmail = ConsoleMenu.readLine(scanner, "CallCenter agent email: ");
        System.out.println("Available sections: " + java.util.Arrays.toString(SupportSection.values()));
        String raw = ConsoleMenu.readLine(scanner, "Sections (comma-separated): ");
        java.util.Set<SupportSection> sections = java.util.EnumSet.noneOf(SupportSection.class);
        if (!parseSections(raw, sections)) {
            return;
        }
        try {
            AdminManagementService.assignSupportSections(actor, agentEmail, sections);
            ConsoleColor.printSuccess("Sections assigned to " + agentEmail + ": " + sections);
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static boolean parseSections(String raw, java.util.Set<SupportSection> sections) {
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                sections.add(SupportSection.valueOf(trimmed.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                ConsoleColor.printError("Unknown section: " + trimmed);
                return false;
            }
        }
        return true;
    }

    private static void manageSystemSettings(Scanner scanner, Persona actor) {
        ConsoleMenu.banner("SYSTEM SETTINGS");
        System.out.println("  Borrow Days: " + SystemSettingsService.getBorrowDays());
        System.out.println("  Fine Rate: " + SystemSettingsService.getFineRate());
        System.out.println("  Reservation Days: " + SystemSettingsService.getReservationDays());
        System.out.println("  Max Reservations: " + SystemSettingsService.getMaxReservations());
        ConsoleMenu.option("1", "Edit Borrow Days");
        ConsoleMenu.option("2", "Edit Fine Rate");
        ConsoleMenu.option("3", "Edit Reservation Days");
        ConsoleMenu.option("4", "Edit Max Reservations");
        ConsoleMenu.back();
        String choice = promptChoice(scanner);
        if ("0".equals(choice)) {
            return;
        }
        applySettingChoice(choice, scanner, actor);
    }

    private static void applySettingChoice(String choice, Scanner scanner, Persona actor) {
        int value = ConsoleMenu.readInt(scanner, "New value: ");
        try {
            runSettingAction(choice, actor, value);
            ConsoleColor.printSuccess("Setting updated.");
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void runSettingAction(String choice, Persona actor, int value) {
        switch (choice) {
            case "1":
                SystemSettingsService.updateBorrowDays(actor, value);
                break;
            case "2":
                SystemSettingsService.updateFineRate(actor, value);
                break;
            case "3":
                SystemSettingsService.updateReservationDays(actor, value);
                break;
            case "4":
                SystemSettingsService.updateMaxReservations(actor, value);
                break;
            default:
                throw new ValidationException(INVALID_ENTRY);
        }
    }

    private static void advanceSimulatedDay() {
        int newDay = SimulationClock.advanceDay();
        List<String> charges = LoanService.accrueOverdueDebts(newDay);
        ReservationService.expireReservations(newDay);
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
        try {
            if (approve) {
                RoleRequestService.approve(requestId);
            } else {
                RoleRequestService.reject(requestId);
            }
            ConsoleColor.printSuccess("Request " + requestId + (approve ? " approved." : " rejected."));
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
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

    private static void inspectDatabase() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Personas", PersonaRepository.getAllPersonas().size());
        counts.put("Mail messages", MailRepository.getAllMailMessages().size());
        counts.put("Transactions", TransactionRepository.getAllTransactions().size());
        counts.put("Loans", LoanRepository.getAllLoans().size());
        counts.put("Library items", LibraryItemRepository.getAllLibraryItems().size());
        counts.put("Suppliers", SupplierRepository.getAllSuppliers().size());
        counts.put("Support tickets", SupportTicketRepository.getAllSupportTickets().size());
        counts.put("Role requests", RoleRequestRepository.getAllRoleRequests().size());
        counts.put("Simulated day", SimulationClock.getCurrentDay());
        ConsoleMenu.printCounts("Database Records", counts);
    }
}
