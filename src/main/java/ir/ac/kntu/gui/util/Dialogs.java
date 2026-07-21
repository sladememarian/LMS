package ir.ac.kntu.gui.util;

import java.util.Optional;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

/**
 * Small helper for user-facing notifications. Phase-3 requires that errors,
 * successes and warnings are shown via visual {@link Alert} boxes instead of the
 * console. All methods are safe to call from any thread (they hop to the FX
 * thread when needed).
 */
public final class Dialogs {

    private Dialogs() {
        // utility class
    }

    public static void error(String header, String message) {
        show(AlertType.ERROR, "Error", header, unwrap(message));
    }

    /** Convenience for exceptions surfaced from background tasks. */
    public static void error(String header, Throwable throwable) {
        error(header, describe(throwable));
    }

    public static void info(String header, String message) {
        show(AlertType.INFORMATION, "Information", header, message);
    }

    public static void warn(String header, String message) {
        show(AlertType.WARNING, "Warning", header, message);
    }

    /** Yes/No confirmation. Returns true if the user confirmed. */
    public static boolean confirm(String header, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION, message,
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Please confirm");
        alert.setHeaderText(header);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && ButtonType.YES.equals(result.get());
    }

    private static void show(AlertType type, String title, String header, String message) {
        BackgroundJobs.onFxThread(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private static String describe(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error.";
        }
        String msg = throwable.getMessage();
        return (msg == null || msg.isBlank())
                ? throwable.getClass().getSimpleName()
                : msg;
    }

    private static String unwrap(String message) {
        return (message == null || message.isBlank()) ? "Something went wrong." : message;
    }
}
