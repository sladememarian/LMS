package ir.ac.kntu.gui.util;

import java.util.Optional;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Small helper for user-facing notifications. Phase-3 requires that errors,
 * successes and warnings are shown via visual {@link Alert} boxes instead of the
 * console. All methods are safe to call from any thread (they hop to the FX
 * thread when needed).
 */
public final class Dialogs {

    private static final Duration FADE = Duration.millis(220);
    private static final Duration HOLD = Duration.seconds(4);

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

    /**
     * Shows a non-modal toast that fades in, holds, then fades out — used for
     * ambient notifications that must not interrupt the user with a dialog.
     */
    public static void toast(String title, String message) {
        BackgroundJobs.onFxThread(() -> showToast(title, message));
    }

    private static void showToast(String title, String message) {
        Window owner = focusedWindow();
        if (owner == null) {
            return;
        }
        Label heading = new Label(title);
        heading.getStyleClass().add("toast-title");
        Label body = new Label(message);
        body.getStyleClass().add("toast-body");
        body.setWrapText(true);

        StackPane card = new StackPane(new javafx.scene.layout.VBox(4, heading, body));
        card.getStyleClass().add("toast");
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setMaxWidth(360);

        Popup popup = new Popup();
        popup.getContent().add(card);
        popup.setAutoFix(true);
        card.setOpacity(0);
        popup.show(owner,
                owner.getX() + owner.getWidth() - 400,
                owner.getY() + owner.getHeight() - 120);

        FadeTransition in = new FadeTransition(FADE, card);
        in.setFromValue(0);
        in.setToValue(1);
        PauseTransition hold = new PauseTransition(HOLD);
        FadeTransition out = new FadeTransition(FADE, card);
        out.setFromValue(1);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, hold, out);
        seq.setOnFinished(event -> popup.hide());
        seq.play();
    }

    private static Window focusedWindow() {
        // Prefer the showing+focused window; fall back to any showing window.
        return Window.getWindows().stream()
                .filter(w -> w.isShowing() && w.isFocused())
                .findFirst()
                .orElseGet(() -> Window.getWindows().stream()
                        .filter(Window::isShowing)
                        .findFirst()
                        .orElse(null));
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
