package ir.ac.kntu.gui.view;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.View;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.gui.util.UiTheme;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.sso.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Login screen. Credential checking runs on a background thread so the UI stays
 * responsive; on success a session is created and the user is routed onward.
 */
public class LoginView implements View {

    private final StackPane root = new StackPane();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Sign in");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private Navigator navigator;

    public LoginView() {
        build();
    }

    /** Injected by {@code App} after construction (breaks the init cycle). */
    public void attachNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    private void build() {
        initFields();
        initLoginButton();
        spinner.setMaxSize(22, 22);
        spinner.setVisible(false);

        VBox card = buildCard();
        root.getChildren().add(card);
        root.getStyleClass().add("auth-bg");
        StackPane.setAlignment(card, Pos.CENTER);
    }

    private void initFields() {
        emailField.setPromptText("Email");
        emailField.setId("emailField");
        emailField.getStyleClass().add("field");
        passwordField.setPromptText("Password");
        passwordField.setId("passwordField");
        passwordField.getStyleClass().add("field");
    }

    private void initLoginButton() {
        loginButton.getStyleClass().add("primary");
        loginButton.setId("loginButton");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> handleLogin());
    }

    private VBox buildCard() {
        Label heading = new Label("KNTU Library");
        heading.getStyleClass().add("h1");
        Label subtitle = new Label("Sign in to your account");
        subtitle.getStyleClass().add("muted");

        Hyperlink createAccount = new Hyperlink("Create a new account");
        createAccount.setOnAction(event -> goToRegister());

        Button themeToggle = new Button("Toggle theme");
        themeToggle.getStyleClass().add("ghost");
        themeToggle.setOnAction(event -> {
            if (navigator != null) {
                navigator.toggleTheme();
            }
        });

        HBox buttonRow = new HBox(10, loginButton, spinner);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, heading, subtitle, emailField, passwordField,
                buttonRow, createAccount, themeToggle);
        card.getStyleClass().add("card");
        card.setMaxWidth(360);
        card.setPadding(new Insets(28));
        return card;
    }

    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            Dialogs.warn("Missing information", "Please enter both email and password.");
            return;
        }

        setBusy(true);
        BackgroundJobs.run(
                () -> {
                    boolean ok = PersonaService.validateCredentials(email, password);
                    if (!ok) {
                        throw new IllegalArgumentException("Invalid email or password.");
                    }
                    return PersonaService.getProfile(email);
                },
                persona -> {
                    setBusy(false);
                    onAuthenticated(persona);
                },
                error -> {
                    setBusy(false);
                    Dialogs.error("Sign-in failed", error);
                });
    }

    private void onAuthenticated(Persona persona) {
        if (persona == null) {
            Dialogs.error("Sign-in failed", "Account could not be loaded.");
            return;
        }
        SessionManager.createSession(persona);
        Persona.setCurrentUser(persona);

        // Apply the user's saved theme, if any.
        if (navigator != null) {
            navigator.setTheme(UiTheme.from(persona.getTheme()));
            navigator.switchTo(new HomePlaceholderView(navigator, persona));
        }
        passwordField.clear();
    }

    private void goToRegister() {
        if (navigator != null) {
            RegisterView register = new RegisterView(navigator);
            navigator.switchTo(register);
        }
    }

    private void setBusy(boolean busy) {
        spinner.setVisible(busy);
        loginButton.setDisable(busy);
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public String title() {
        return "Sign in";
    }
}
