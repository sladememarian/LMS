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
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.reservation.Reservation;
import ir.ac.kntu.reservation.ReservationService;

/**
 * Computes user notifications (due-soon loans and active reservations) on a
 * background thread and surfaces them without blocking the UI. Per the Phase-3
 * note, this is triggered at Login and on "Next Day" — not by a permanently
 * running thread.
 */
public final class NotificationChecker {

    private static final int DUE_SOON_THRESHOLD_DAYS = 3;

    private NotificationChecker() {
    }

    /** Runs the check off the FX thread and, if anything is found, shows a summary. */
    public static void checkAndNotify(Persona persona) {
        if (persona == null) {
            return;
        }
        BackgroundJobs.run(
                () -> computeWarnings(persona),
                warnings -> {
                    if (warnings != null && !warnings.isEmpty()) {
                        Dialogs.toast("Notifications", String.join("\n", warnings));
                    }
                },
                error -> { /* silent: notifications must never disrupt the user */ });
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
