package ir.ac.kntu.gui;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Shared TestFX helpers for the GUI end-to-end tests.
 *
 * <p>Login now goes through the real two-factor flow (a background job delivers
 * a code to the simulated inbox, then a modal dialog asks for it), so a plain
 * "type credentials, click Sign in, expect the shell" no longer works. Every
 * GUI test drives that flow through {@link #signIn(FxRobot, String)}, which
 * relies only on already-shipped behaviour: the {@code .env} master key
 * ({@code MASTER_KEY=bid}) skips the real password and the master OTP
 * ({@code MASTER_OTP=123}) skips reading the inbox.
 */
final class GuiTestSupport {

    /** {@code .env} master key: any existing account signs in with this password. */
    static final String MASTER_KEY = "bid";
    /** {@code .env} master OTP: always passes 2FA verification. */
    static final String MASTER_OTP = "123";

    private static final String TWO_FA_HEADER = "Enter the verification code sent to your inbox.";
    private static final String OK_BUTTON = "OK";
    // Generous: the first sign-in in a class pays a cold-start cost (JavaFX +
    // DB + MailService init) before the 2FA background job delivers a code, so
    // a tight budget flakes on whichever test JUnit happens to run first. 60s
    // comfortably covers a cold Gradle daemon running a single GUI test.
    private static final int TIMEOUT_SECONDS = 60;

    private GuiTestSupport() {
        // static helpers only
    }

    /**
     * Signs {@code email} in end-to-end: master-key credentials, then the 2FA
     * dialog answered with the master OTP, leaving the {@code AppShell} visible.
     */
    static void signIn(FxRobot robot, String email) {
        robot.clickOn("#emailField").write(email);
        robot.clickOn("#passwordField").write(MASTER_KEY);
        robot.clickOn("#loginButton");

        // Credentials are validated on a background thread, then a second job
        // delivers the 2FA code and opens the inbox before the verification
        // dialog appears — so the dialog shows a moment after the click. Wait for
        // its header before typing. If a background job errors instead, an alert
        // pops; detect that and surface its text rather than timing out silently.
        try {
            waitFor(() -> robot.lookup(TWO_FA_HEADER).tryQuery().isPresent()
                    || robot.lookup(".alert").tryQuery().isPresent());
        } catch (AssertionError timeout) {
            throw new AssertionError("2FA/alert never appeared within the sign-in budget. "
                    + dumpWindows(), timeout);
        }
        if (!robot.lookup(TWO_FA_HEADER).tryQuery().isPresent()) {
            throw new AssertionError("2FA dialog never appeared; a sign-in error alert showed instead. "
                    + dumpWindows());
        }

        // The TextInputDialog's editor is the focused field; type the master OTP
        // straight into it, then confirm. The dialog runs its own nested event
        // loop, so clicking OK returns control once verification is dispatched.
        robot.write(MASTER_OTP);
        robot.clickOn(OK_BUTTON);

        // Verification runs on a background thread and, on success, swaps in the
        // post-login shell. Wait for its sidebar before returning.
        waitFor(() -> robot.lookup(".sidebar").tryQuery().isPresent());
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Dismisses a success/info {@link javafx.scene.control.Alert} whose header
     * is {@code header} (e.g. "Borrowed", "Reserved", "Returned"). The alert is
     * raised from a background job's success callback, so this waits for it.
     */
    static void dismissInfo(FxRobot robot, String header) {
        waitFor(() -> robot.lookup(header).tryQuery().isPresent());
        robot.clickOn(OK_BUTTON);
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Waits until {@code text} appears anywhere in the UI, then returns. */
    static void waitForText(FxRobot robot, String text) {
        waitFor(() -> robot.lookup(text).tryQuery().isPresent());
    }

    private static void waitFor(Callable<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS, condition);
        } catch (Exception e) {
            throw new AssertionError("Timed out waiting for a UI condition", e);
        }
    }

    /** Diagnostic: lists every showing window and its root text. */
    private static String dumpWindows() {
        StringBuilder sb = new StringBuilder("Windows: ");
        final StringBuilder collector = sb;
        try {
            org.testfx.util.WaitForAsyncUtils.asyncFx(() -> {
                for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                    collector.append("[showing=").append(w.isShowing())
                             .append(" cls=").append(w.getClass().getSimpleName());
                    if (w.getScene() != null && w.getScene().getRoot() != null) {
                        collector.append(" text=")
                                 .append(textOf(w.getScene().getRoot()));
                    }
                    collector.append("] ");
                }
                return null;
            }).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            sb.append("(dump failed: ").append(e).append(")");
        }
        return sb.toString();
    }

    private static String textOf(javafx.scene.Node node) {
        StringBuilder sb = new StringBuilder();
        if (node instanceof javafx.scene.control.Labeled) {
            String t = ((javafx.scene.control.Labeled) node).getText();
            if (t != null && !t.isBlank()) {
                sb.append('"').append(t).append('"');
            }
        }
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                sb.append(textOf(child));
            }
        }
        return sb.toString();
    }
}
