package ir.ac.kntu.gui.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.reservation.Reservation;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.support.notification.NotificationService;

// Computes user notifications (due-soon loans and ready-to-borrow reservations)
// on a background thread and surfaces them without blocking the UI. Per the
// Phase-3 note, this is triggered at Login and on "Next Day" — not by a
// permanently running thread.
//
// A ready reservation also gets persisted once into the user's Notifications
// tab (mail inbox), so the "go grab your item" alert survives after the toast
// fades. Dedup keeps it to a single saved notification per reservation.
public final class NotificationChecker {

    private static final int DUE_SOON_THRESHOLD_DAYS = 3;
    // Marker embedded in the saved notification's subject so we can tell which
    // reservations already have a persisted "ready" alert and never save a
    // duplicate on the next login/day check.
    private static final String READY_SUBJECT_PREFIX = "Reservation ready: ";

    private NotificationChecker() {
    }

    // Runs the check off the FX thread, persists any ready-reservation alerts,
    // and shows a summary toast if anything is found.
    public static void checkAndNotify(Persona persona) {
        if (persona == null) {
            return;
        }
        BackgroundJobs.run(
                () -> {
                    persistReadyReservations(persona);
                    return computeWarnings(persona);
                },
                warnings -> {
                    if (warnings != null && !warnings.isEmpty()) {
                        Dialogs.toast("Notifications", String.join("\n", warnings));
                    }
                },
                error -> { /* silent: notifications must never disrupt the user */ });
    }

    // Saves a persistent "your reservation is active, go grab your item"
    // notification for every ACTIVE reservation that doesn't already have one.
    // Runs on the background thread; only touches the mail store.
    private static void persistReadyReservations(Persona persona) {
        String address = persona.getEmail() != null
                ? persona.getEmail() : persona.getUsername();
        if (address == null) {
            return;
        }
        java.util.Set<String> alreadyNotified = MailService.getInbox(address).getMessages()
                .stream()
                .map(MailMessage::getSubject)
                .filter(subject -> subject != null && subject.startsWith(READY_SUBJECT_PREFIX))
                .collect(Collectors.toSet());

        for (Reservation reservation : ReservationService.getMemberReservations(persona.getMemberId())) {
            if (!reservation.isActive()) {
                continue;
            }
            String subject = READY_SUBJECT_PREFIX + reservation.getReservationId();
            if (alreadyNotified.contains(subject)) {
                continue;
            }
            LibraryItem item = LibraryService.getItemById(reservation.getItemId());
            String title = item != null ? item.getTitle() : reservation.getItemId();
            String body = "\"" + title + "\" is ready to pick up. Please grab your item"
                    + " by day " + reservation.getExpiresOnDay() + ".";
            NotificationService.notify(persona, subject, body);
        }
    }

    /** Pure computation (Streams) — safe to run on a background thread. */
    public static List<String> computeWarnings(Persona persona) {
        List<String> messages = new ArrayList<>();
        String memberId = persona.getMemberId();
        int today = SimulationClock.getCurrentDay();

        // Loans due within the threshold (Streams).
        List<String> dueSoon = LoanService.getLoans().stream()
                .filter(loan -> memberId != null && memberId.equals(loan.getMemberId()))
                .filter(loan -> {
                    int remaining = loan.getDueDay() - today;
                    return remaining >= 0 && remaining < DUE_SOON_THRESHOLD_DAYS;
                })
                .map(loan -> {
                    LibraryItem item = LibraryService.getItemById(loan.getItemId());
                    String title = item != null ? item.getTitle() : loan.getItemId();
                    int remaining = loan.getDueDay() - today;
                    return "• \"" + title + "\" is due in " + remaining + " day(s).";
                })
                .collect(Collectors.toList());
        messages.addAll(dueSoon);

        // Active (ready) reservations (Streams).
        List<Reservation> reservations = ReservationService.getMemberReservations(memberId);
        long readyCount = reservations.stream()
                .filter(Reservation::isActive)
                .count();
        if (readyCount > 0) {
            messages.add("• " + readyCount + " of your reservation(s) are now ready to borrow.");
        }
        return messages;
    }
}
