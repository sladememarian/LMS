package ir.ac.kntu.support.inbox;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.persona.AdminManagementService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class AdminUserManagement {
    private static final String PROMPT_EMAIL = "Email: ";
    private static final String INVALID_ENTRY = "Invalid entry.";
    private static final String SEP = " | ";

    private AdminUserManagement() {
    }

    public static void open(Scanner scanner, Persona actor) {
        ConsoleMenu.banner("USER MANAGEMENT");
        ConsoleMenu.option("1", "View All Users");
        ConsoleMenu.option("2", "Search Users");
        ConsoleMenu.option("3", "Edit User Profile");
        ConsoleMenu.option("4", "Promote / Demote User");
        ConsoleMenu.option("5", "Reset Password");
        ConsoleMenu.option("6", "Activate / Deactivate User");
        ConsoleMenu.back();
        String choice = ConsoleMenu.readLine(scanner,
                ConsoleColor.YELLOW + "Choose: " + ConsoleColor.RESET);
        try {
            dispatch(choice, scanner, actor);
        } catch (BaseException ex) {
            ConsoleColor.printError(ex.getMessage());
        }
    }

    private static void dispatch(String choice, Scanner scanner, Persona actor) {
        switch (choice) {
            case "1":
                listAllUsersAction();
                break;
            case "2":
                searchUsersAction(scanner);
                break;
            case "3":
                editUserProfileAction(scanner, actor);
                break;
            case "4":
                promoteDemoteAction(scanner, actor);
                break;
            case "5":
                resetPasswordAction(scanner, actor);
                break;
            case "6":
                toggleActiveAction(scanner, actor);
                break;
            default:
                ConsoleColor.printError(INVALID_ENTRY);
                break;
        }
    }

    private static void listAllUsersAction() {
        List<Persona> users = AdminManagementService.listAllUsers();
        if (users.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no users found)"));
            return;
        }
        for (Persona p : users) {
            System.out.println(formatUserLine(p));
        }
    }

    private static void searchUsersAction(Scanner scanner) {
        String keyword = ConsoleMenu.readLine(scanner, "Search (name / email / ID / role): ");
        List<Persona> results = AdminManagementService.searchUsers(keyword);
        if (results.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no matches)"));
            return;
        }
        for (Persona p : results) {
            System.out.println(formatUserLine(p));
        }
    }

    private static void editUserProfileAction(Scanner scanner, Persona actor) {
        if (actor.getRole() != ir.ac.kntu.persona.UserRole.ADMIN) {
            ConsoleColor.printError("Only Admins can edit user profiles.");
            return;
        }
        String email = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        String first = ConsoleMenu.readLine(scanner, "First name: ");
        String last = ConsoleMenu.readLine(scanner, "Last name: ");
        String phone = ConsoleMenu.readLine(scanner, "Phone: ");
        AdminManagementService.editUserProfile(email, first, last, phone);
        ConsoleColor.printSuccess("Profile updated for: " + email);
    }

    private static void promoteDemoteAction(Scanner scanner, Persona actor) {
        String email = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        System.out.println("Roles: ADMIN, CALLCENTER, TEACHER, STUDENT, GUEST");
        String roleStr = ConsoleMenu.readLine(scanner, "New role: ").toUpperCase();
        ir.ac.kntu.persona.UserRole newRole = ir.ac.kntu.persona.UserRole.valueOf(roleStr);
        AdminManagementService.promoteAdmin(actor, email, newRole);
        ConsoleColor.printSuccess("Role updated for: " + email + " -> " + newRole);
    }

    private static void resetPasswordAction(Scanner scanner, Persona actor) {
        String targetEmail = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        String newPass = ConsoleMenu.readLine(scanner, "New password: ");
        AdminManagementService.resetPassword(actor, targetEmail, newPass);
        ConsoleColor.printSuccess("Password reset for: " + targetEmail);
    }

    private static void toggleActiveAction(Scanner scanner, Persona actor) {
        String email = ConsoleMenu.readLine(scanner, PROMPT_EMAIL);
        boolean nowActive = AdminManagementService.toggleActive(actor, email);
        String status = nowActive ? "activated" : "deactivated";
        ConsoleColor.printSuccess("Account " + status + " for: " + email);
    }

    private static String formatUserLine(Persona persona) {
        String first = persona.getFirstName() == null ? "-" : persona.getFirstName();
        String last = persona.getLastName() == null ? "-" : persona.getLastName();
        String email = persona.getEmail() == null ? "-" : persona.getEmail();
        String name = first + " " + last;
        String active = persona.isActive() ? "ACTIVE" : "INACTIVE";
        return ConsoleColor.CYAN + "  " + persona.getMemberId() + ConsoleColor.RESET
                + SEP + name.trim()
                + SEP + email
                + SEP + persona.getRole().name()
                + SEP + active;
    }
}
