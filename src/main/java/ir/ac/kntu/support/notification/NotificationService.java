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

/**
 * Support notification centre. It does not own its own message store; it reuses
 * the Mail microservice (SYSTEM_NOTIFICATION messages) so there is a single
 * source of truth for everything delivered to a user.
 */
public class NotificationService {

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

    public static void showNotifications(Scanner scanner, Persona user) {
        String address = address(user);
        Inbox inbox = MailService.getInbox(address);
        List<MailMessage> messages = inbox.getMessages();
        ConsoleMenu.banner("NOTIFICATIONS: " + address);
        boolean any = false;
        for (MailMessage message : messages) {
            if (message.getMessageType() == MessageType.SYSTEM_NOTIFICATION) {
                System.out.println(ConsoleColor.gray("  " + message.getSubject() + " :: " + message.getBody()));
                any = true;
            }
        }
        if (!any) {
            System.out.println(ConsoleColor.gray("  (no notifications)"));
        }
        MailService.markInboxRead(address);
        ConsoleMenu.pause(scanner);
    }

    private static String address(Persona persona) {
        return persona.getEmail() != null ? persona.getEmail() : persona.getUsername();
    }
}
