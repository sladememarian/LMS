package ir.ac.kntu.gui;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.BrandAssets;
import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.SplashView;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

// JavaFX entry point. Builds the Navigator, shows the login screen, and shuts
// down the background worker pool on exit. All business services (IAM, Library,
// Finance, Support, ...) are reused unchanged; this only bootstraps the shell.
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnnecessaryConstructor"})
public class App extends Application {

    public App() {
        super();
    }

    @Override
    public void start(Stage primaryStage) {
        // The login view and navigator have a circular dependency, so the view
        // is created first and the navigator ref is injected afterwards.
        LoginView login = new LoginView();

        // Boot on the splash: it simulates loading from postgres, then hands off
        // to the login screen. The navigator starts on the splash so the very
        // first thing shown is the brand loading bar.
        SplashView splash = new SplashView();
        Navigator navigator = new Navigator(primaryStage, splash);
        login.attachNavigator(navigator);
        splash.attachNavigator(navigator, login);

        // Brand the window/taskbar with the Collaberry icon instead of the
        // JavaFX default. Skipped silently if the asset is missing.
        Image icon = BrandAssets.loadIcon();
        if (icon != null) {
            primaryStage.getIcons().add(icon);
        }

        primaryStage.setOnCloseRequest(event -> BackgroundJobs.shutdown());
        primaryStage.show();
        bringToFront(primaryStage);
        splash.start();
    }

    // On Windows a freshly launched stage sometimes appears behind the terminal
    // that started it. A momentary always-on-top toggle plus toFront() pulls it
    // forward on launch without pinning it above every other window afterwards.
    private void bringToFront(Stage stage) {
        stage.setAlwaysOnTop(true);
        stage.toFront();
        stage.requestFocus();
        stage.setAlwaysOnTop(false);
    }

    @Override
    public void stop() {
        BackgroundJobs.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
