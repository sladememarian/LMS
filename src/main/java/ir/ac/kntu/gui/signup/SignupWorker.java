package ir.ac.kntu.gui.signup;

import ir.ac.kntu.exception.BaseException;
import ir.ac.kntu.persona.PersonaService;

/**
 * The consumer thread for {@link SignupQueue}.
 *
 * <p>A single daemon thread loops forever: it blocks on {@link SignupQueue#take()}
 * until an envelope arrives, then persists the profile fields by calling the
 * existing {@code PersonaService.updateProfile}. Because the fast path (create
 * account with email + password) and this slow path (attach the rest of the
 * profile) run on <em>different</em> threads, the account row may not be visible
 * the instant the worker wakes up; the worker therefore retries a bounded number
 * of times before giving up on a single envelope, and never lets one bad
 * envelope kill the loop.</p>
 *
 * <p>Started once via {@link #start()} (idempotent). Lives entirely in the GUI
 * layer; the only backend call is the pre-existing {@code updateProfile}.</p>
 */
public final class SignupWorker {

    private static final int MAX_ATTEMPTS = 20;
    private static final long RETRY_DELAY_MILLIS = 150L;

    private static volatile boolean started;

    private final SignupQueue queue;

    private SignupWorker(SignupQueue queue) {
        this.queue = queue;
    }

    /** Starts the singleton worker thread once; further calls are no-ops. */
    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        SignupWorker worker = new SignupWorker(SignupQueue.getInstance());
        Thread thread = new Thread(worker::runLoop, "signup-profile-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void runLoop() {
        while (true) {
            SignupEnvelope envelope;
            try {
                envelope = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                process(envelope);
            } catch (BaseException e) {
                // Never let one envelope kill the consumer loop.
                System.err.println("SignupWorker: dropping envelope for "
                        + envelope.getEmail() + ": " + e.getMessage());
                queue.markProcessed(envelope);
            }
        }
    }

    private void process(SignupEnvelope envelope) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                PersonaService.updateProfile(
                        envelope.getEmail(),
                        envelope.getFirstName(),
                        envelope.getLastName(),
                        envelope.getPhoneNumber());
                queue.markProcessed(envelope);
                return;
            } catch (BaseException e) {
                // Most likely the account row is not visible yet (the register
                // thread is still in flight). Back off briefly and retry.
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                sleepQuietly();
            }
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
