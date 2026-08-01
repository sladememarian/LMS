package ir.ac.kntu.gui.signup;

import java.util.function.Consumer;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.persona.PersonaService;

/**
 * GUI-side orchestrator for the two-stage asynchronous sign-up.
 *
 * <p>Stage 1 (fast, on a {@link BackgroundJobs} thread): create the account with
 * just email + password via {@code PersonaService.registerPersona} so the user
 * can sign in immediately. Stage 2 (deferred): the remaining profile fields are
 * wrapped in a {@link SignupEnvelope} and handed to the durable {@link
 * SignupQueue}; the separate {@link SignupWorker} thread later persists them via
 * {@code PersonaService.updateProfile}.</p>
 *
 * <p>The producer (register + enqueue) and the consumer (profile persistence)
 * therefore run on different threads, which is what keeps the interactive
 * sign-up snappy. No backend code is modified — only existing services are
 * called.</p>
 */
public final class SignupService {

    private SignupService() {
        // utility class
    }

    /**
     * Registers the account and schedules profile persistence.
     *
     * @param profile        the collected sign-up fields (email + name + phone).
     * @param password       the account password (kept off the durable envelope).
     * @param onAccountReady invoked on the FX thread once the account exists and
     *                       the profile envelope has been queued (login is now
     *                       possible).
     * @param onError        invoked on the FX thread if account creation fails.
     */
    public static void submit(SignupEnvelope profile,
                              String password,
                              Runnable onAccountReady,
                              Consumer<Throwable> onError) {
        // Make sure the consumer thread is running before we produce work.
        SignupWorker.start();

        String email = profile.getEmail();
        BackgroundJobs.runAction(
                () -> {
                    // Stage 1 — fast path: only email + password hit the account store.
                    PersonaService.registerPersona(email, password);
                    // Stage 2 — enqueue the rest for the worker thread to persist.
                    SignupQueue.getInstance().enqueue(profile);
                },
                onAccountReady,
                onError);
    }
}
