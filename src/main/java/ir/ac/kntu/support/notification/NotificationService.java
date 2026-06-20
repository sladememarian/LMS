package ir.ac.kntu.support.notification;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class NotificationService {
    // spamming users with notifications since day one
    public static void notify(Persona recipient, String subject, String body) {
        if (recipient != null) {
            MailService.sendSystemNotification(address(recipient), subject, body);
        }
    }

    public static void notifyAddress(String address, String subject, String body) {
        if (address != null && !address.isEmpty()) {
            MailService.sendSystemNotification(address, subject, body);
        }
    }

    private static List<MailMessage> filterNotifications(Inbox inbox) {
        List<MailMessage> result = new java.util.ArrayList<>();
        for (MailMessage msg : inbox.getMessages()) {
            if (msg.getMessageType() == MessageType.SYSTEM_NOTIFICATION) {
                result.add(msg);
            }
        }
        return result;
    }

    private static void printNotificationPage(List<MailMessage> notifications, int page, int totalPages) {
        int pageSize = 10;
        int start = page * pageSize;
        int end = Math.min(start + pageSize, notifications.size());
        System.out.println(ConsoleColor.BOLD + "--- Page " + (page + 1) + "/" + totalPages
                + " (" + notifications.size() + " notifications) ---" + ConsoleColor.RESET);
        for (int i = start; i < end; i++) {
            MailMessage msg = notifications.get(i);
            System.out.println(ConsoleColor.gray("  " + msg.getSubject() + " :: " + msg.getBody()));
        }
    }

    private static int navigateNotifications(Scanner scanner, int currentPage, int totalPages) {
        System.out.print(ConsoleColor.YELLOW + "[N]ext page  [P]revious page  [Q]uit: " + ConsoleColor.RESET);
        String cmd = scanner.nextLine().trim().toUpperCase();
        if ("N".equals(cmd) && currentPage < totalPages - 1) {
            return currentPage + 1;
        }
        if ("P".equals(cmd) && currentPage > 0) {
            return currentPage - 1;
        }
        return currentPage;
    }

    public static void showNotifications(Scanner scanner, Persona user) {
        String address = address(user);
        List<MailMessage> notifications = filterNotifications(MailService.getInbox(address));
        ConsoleMenu.banner("NOTIFICATIONS: " + address);
        if (notifications.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no notifications)"));
            MailService.markInboxRead(address);
            ConsoleMenu.pause(scanner);
            return;
        }
        int totalPages = (notifications.size() + 9) / 10;
        int currentPage = 0;
        while (true) {
            printNotificationPage(notifications, currentPage, totalPages);
            if (totalPages <= 1) {
                break;
            }
            int next = navigateNotifications(scanner, currentPage, totalPages);
            if (next == currentPage) {
                break;
            }
            currentPage = next;
        }
        MailService.markInboxRead(address);
    }

    private static String address(Persona persona) {
        return persona.getEmail() != null ? persona.getEmail() : persona.getUsername();
    }
}