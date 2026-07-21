package ir.ac.kntu.gui;

import ir.ac.kntu.gui.util.UiTheme;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Owns the primary {@link Stage} and performs multi-scene navigation by swapping
 * the root of a single reused {@link Scene}. Keeping one Scene lets the active
 * theme stylesheet persist across screen changes.
 *
 * <p>A single shared instance is created by {@link App} and passed to every
 * view, so any screen can navigate to another via {@code navigator.switchTo(...)}.
 */
public final class Navigator {

    private final Stage stage;
    private final Scene scene;
    private UiTheme theme = UiTheme.LIGHT;

    public Navigator(Stage stage, View initial) {
        this.stage = stage;
        this.scene = new Scene(initial.getRoot(), 1024, 720);
        theme.applyTo(scene);
        stage.setScene(scene);
        stage.setTitle(title(initial));
        stage.setMinWidth(880);
        stage.setMinHeight(600);
    }

    /** Replaces the current screen with {@code view}. */
    public void switchTo(View view) {
        scene.setRoot(view.getRoot());
        stage.setTitle(title(view));
    }

    public void setTheme(UiTheme newTheme) {
        this.theme = newTheme;
        theme.applyTo(scene);
    }

    public UiTheme getTheme() {
        return theme;
    }

    public void toggleTheme() {
        setTheme(theme == UiTheme.LIGHT ? UiTheme.DARK : UiTheme.LIGHT);
    }

    public Stage getStage() {
        return stage;
    }

    private static String title(View view) {
        return "KNTU Library — " + view.title();
    }
}
