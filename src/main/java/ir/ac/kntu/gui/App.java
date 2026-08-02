package ir.ac.kntu.gui;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.SplashView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application entry point. Builds the {@link Navigator}, shows the login
 * screen, and cleans up the background worker pool on exit.
 *
 * <p>All existing business services (IAM, Library, Finance, Support, ...) are
 * reused unchanged; this class only bootstraps the graphical shell.
 */
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

        primaryStage.setOnCloseRequest(event -> BackgroundJobs.shutdown());
        primaryStage.show();
        splash.start();
    }

    @Override
    public void stop() {
        BackgroundJobs.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
