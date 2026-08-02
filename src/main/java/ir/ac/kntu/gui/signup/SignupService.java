package ir.ac.kntu.gui.signup;

import java.util.function.Consumer;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.persona.PersonaService;

/**
 * GUI-side orchestrator for the three-stage asynchronous sign-up.
 *
 * <p>The interactive path is split into three concerns that run on independent
 * threads so the user gets an "account created" window the instant their
 * credentials are accepted, and none of the slower work blocks the UI:</p>
 * <ul>
 *   <li><b>Stage A — create the account</b> (on a {@link BackgroundJobs}
 *       thread): {@code PersonaService.registerPersona(email, password)} writes
 *       the identity row to the store (Postgres in prod, H2 in tests). This is
 *       the only thing the user must wait for before they can sign in.</li>
 *   <li><b>Stage B — GUI trigger</b> (FX thread): as soon as Stage A reports the
 *       credentials are accepted, {@code onAccountReady} fires and the "Account
 *       created" window opens. It does <em>not</em> wait for the profile to be
 *       persisted.</li>
 *   <li><b>Stage C — profile persistence</b> (independent): the remaining
 *       fields (first/last name, phone) are wrapped in a {@link SignupEnvelope}
 *       and handed to the durable {@link SignupQueue}; the {@link SignupWorker}
 *       thread later drains it via {@code PersonaService.updateProfile}.</li>
 * </ul>
 *
 * <p>Stage C is scheduled independently of Stage B, so a queue/disk hiccup can
 * never surface as a "registration failed" error once the account already
 * exists — the failure is logged and the durable spool still carries the
 * envelope. No backend code is modified — only existing services are called.</p>
 */
public final class SignupService {

    private SignupService() {
        // utility class
    }

    /**
     * Registers the account (Stage A) and, once credentials are accepted, both
     * fires the GUI trigger (Stage B) and schedules profile persistence
     * (Stage C) independently.
     *
     * @param profile        the collected sign-up fields (email + name + phone).
     * @param password       the account password (kept off the durable envelope).
     * @param onAccountReady invoked on the FX thread the moment the account
     *                       exists and login is possible (Stage B). Fires
     *                       independently of profile persistence.
     * @param onError        invoked on the FX thread only if account creation
     *                       (Stage A) fails; profile-queue failures never reach
     *                       it.
     */
    public static void submit(SignupEnvelope profile,
                              String password,
                              Runnable onAccountReady,
                              Consumer<Throwable> onError) {
        // Make sure the consumer thread is running before we produce work.
        SignupWorker.start();

        String email = profile.getEmail();
        BackgroundJobs.runAction(
                // Stage A — create the account with just email + password.
                () -> PersonaService.registerPersona(email, password),
                () -> {
                    // Credentials accepted. Stage C: hand the profile off for
                    // deferred persistence, independently of the GUI trigger, so
                    // enqueue work never delays the "account created" window and
                    // a queue failure never masks the successful registration.
                    enqueueProfile(profile);
                    // Stage B — GUI trigger: login is now possible.
                    if (onAccountReady != null) {
                        onAccountReady.run();
                    }
                },
                onError);
    }

    /**
     * Stage C producer. Enqueues the profile envelope for the worker to persist,
     * swallowing (and logging) any failure: the account already exists, so a
     * queue problem must not be reported as a sign-up failure.
     */
    private static void enqueueProfile(SignupEnvelope profile) {
        try {
            SignupQueue.getInstance().enqueue(profile);
        } catch (RuntimeException e) {
            System.err.println("[SignupService] profile enqueue failed for "
                    + profile.getEmail() + "; account exists, profile deferred: " + e.getMessage());
        }
    }
}
