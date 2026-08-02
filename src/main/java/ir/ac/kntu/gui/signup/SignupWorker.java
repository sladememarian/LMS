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
    // Short back-off: the register thread is usually done within a few
    // milliseconds, so a tight retry keeps profile persistence snappy while
    // still tolerating the brief window where the account row isn't visible yet.
    private static final long RETRY_DELAY_MILLIS = 50L;

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
        System.out.println("[SignupWorker] consumer started on thread="
                + Thread.currentThread().getName());
        // Control-flow loop: the consumer must live for the whole app, blocking
        // in take() between envelopes. Not a collection to stream over.
        while (true) {
            SignupEnvelope envelope;
            try {
                envelope = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            SignupLog.step(SignupLog.THREAD_C, "worker is working on queue for "
                    + envelope.getEmail());
            try {
                process(envelope);
            } catch (BaseException e) {
                // Never let one envelope kill the consumer loop.
                SignupLog.fail(SignupLog.THREAD_C, "dropping envelope for "
                        + envelope.getEmail() + " after retries: " + e.getMessage());
                queue.markProcessed(envelope);
            }
        }
    }

    private void process(SignupEnvelope envelope) {
        long start = System.nanoTime();
        // Control-flow loop: bounded retry while the account row becomes visible;
        // this is back-off logic, not a collection traversal.
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                PersonaService.updateProfile(
                        envelope.getEmail(),
                        envelope.getFirstName(),
                        envelope.getLastName(),
                        envelope.getPhoneNumber());
                queue.markProcessed(envelope);
                long millis = (System.nanoTime() - start) / 1_000_000L;
                SignupLog.step(SignupLog.THREAD_C, "envelope data successfully saved in db for "
                        + envelope.getEmail() + " (" + millis + "ms, attempt " + attempt
                        + ", pending=" + queue.pendingCount() + ")");
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
