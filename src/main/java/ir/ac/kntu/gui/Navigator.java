package ir.ac.kntu.gui;

import ir.ac.kntu.gui.util.UiTheme;
import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

// Owns the primary Stage and navigates by swapping the root of one reused Scene.
// Keeping a single Scene lets the active theme stylesheet persist across screen
// changes. App creates one shared instance and passes it to every view, so any
// screen can navigate via navigator.switchTo(...).
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

    // Replaces the current screen with view, with a short fade-in.
    public void switchTo(View view) {
        javafx.scene.Parent root = view.getRoot();
        scene.setRoot(root);
        stage.setTitle(title(view));
        FadeTransition fade = new FadeTransition(Duration.millis(220), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
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
