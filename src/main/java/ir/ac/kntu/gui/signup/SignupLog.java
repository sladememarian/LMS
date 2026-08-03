package ir.ac.kntu.gui.signup;

// Tiny terminal narrator for the three-stage async sign-up, so the flow is
// observable on the console as it hops across threads:
//   thread-A (lms-bg-worker) — creates the account: validate -> initialised -> saved.
//   thread-B (FX thread) — opens the "Account created" window once credentials pass.
//   thread-C (producer on FX thread, then the signup-profile-worker consumer) —
//     builds the profile envelope, injects it in the queue, and persists it.
// Every line prints the real executing thread name via on=… so the narrative
// stays honest even though the labels are stage-oriented. Pure logging.
public final class SignupLog {

    // Stage labels used across the sign-up package.
    public static final String THREAD_A = "thread-A";
    public static final String THREAD_B = "thread-B";
    public static final String THREAD_C = "thread-C";

    private SignupLog() {
        // utility class
    }

    // Narrates a normal step, e.g. step(THREAD_A, "iam accepted").
    public static void step(String stage, String message) {
        System.out.println("[signup] " + stage + ": " + message
                + " | on=" + Thread.currentThread().getName());
    }

    // Narrates a failure/negative path on the same stage, to stderr.
    public static void fail(String stage, String message) {
        System.err.println("[signup] " + stage + " FAILED: " + message
                + " | on=" + Thread.currentThread().getName());
    }
}
