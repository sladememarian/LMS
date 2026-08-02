package ir.ac.kntu.gui.signup;

/**
 * Tiny terminal narrator for the three-stage async sign-up, so the flow is
 * observable on the console as it hops across threads:
 *
 * <ul>
 *   <li><b>thread-A</b> ({@code lms-bg-worker}) — creates the account:
 *       validate → account initialised → saved in db.</li>
 *   <li><b>thread-B</b> (FX thread) — opens the "Account created" window the
 *       moment the credentials are accepted.</li>
 *   <li><b>thread-C</b> (producer on the FX thread, then the
 *       {@code signup-profile-worker} consumer) — builds the profile envelope,
 *       injects it in the queue, and the worker persists it in the db.</li>
 * </ul>
 *
 * <p>Every line prints the <em>real</em> executing thread name via {@code on=…}
 * so the narrative stays honest even though the labels are stage-oriented. This
 * is pure logging — it changes no behaviour.</p>
 */
public final class SignupLog {

    /** Stage labels used across the sign-up package. */
    public static final String THREAD_A = "thread-A";
    public static final String THREAD_B = "thread-B";
    public static final String THREAD_C = "thread-C";

    private SignupLog() {
        // utility class
    }

    /** Narrates a normal step, e.g. {@code step(THREAD_A, "iam accepted")}. */
    public static void step(String stage, String message) {
        System.out.println("[signup] " + stage + ": " + message
                + " | on=" + Thread.currentThread().getName());
    }

    /** Narrates a failure/negative path on the same stage, to stderr. */
    public static void fail(String stage, String message) {
        System.err.println("[signup] " + stage + " FAILED: " + message
                + " | on=" + Thread.currentThread().getName());
    }
}
