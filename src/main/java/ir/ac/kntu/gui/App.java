package ir.ac.kntu.gui;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.view.LoginView;
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
        // The initial view needs the navigator, and the navigator needs an
        // initial view, so we create the login view with a lazy navigator ref.
        LoginView login = new LoginView();
        Navigator navigator = new Navigator(primaryStage, login);
        login.attachNavigator(navigator);

        primaryStage.setOnCloseRequest(event -> BackgroundJobs.shutdown());
        primaryStage.show();
    }

    @Override
    public void stop() {
        BackgroundJobs.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
