package ir.ac.kntu.gui.view;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.View;
import ir.ac.kntu.gui.component.PasswordBox;
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

    private static final String SIGN_IN_FAILED = "Sign-in failed";

    private final StackPane root = new StackPane();
    private final TextField emailField = new TextField();
    private final PasswordBox passwordField = new PasswordBox();
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
        passwordField.setPromptText("Password");
        passwordField.setFieldId("passwordField");
        emailField.getStyleClass().add("field");
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
                    startTwoFactor(persona);
                },
                error -> {
                    setBusy(false);
                    Dialogs.error(SIGN_IN_FAILED, error);
                });
    }

    /**
     * Second login factor: deliver a 2FA code to the user's simulated mailbox,
     * pop the inbox window so they can read it, then verify the entered code
     * before a session is created. The master OTP (.env {@code MASTER_OTP=123})
     * still passes via {@code MailService.verifyCode}, so testers can skip the
     * inbox. All mail work runs on a background thread.
     */
    private void startTwoFactor(Persona persona) {
        if (persona == null) {
            Dialogs.error(SIGN_IN_FAILED, "Account could not be loaded.");
            return;
        }
        String email = persona.getEmail();
        setBusy(true);
        BackgroundJobs.run(
                () -> ir.ac.kntu.mail.MailService.deliver2FACode(email),
                code -> {
                    setBusy(false);
                    UiTheme theme = navigator != null ? navigator.getTheme() : UiTheme.LIGHT;
                    InboxWindow inbox = new InboxWindow(email, theme);
                    inbox.show();
                    promptForCode(persona, inbox);
                },
                error -> {
                    setBusy(false);
                    Dialogs.error("Could not send 2FA code", error);
                });
    }

    private void promptForCode(Persona persona, InboxWindow inbox) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Two-factor authentication");
        dialog.setHeaderText("Enter the verification code sent to your inbox.");
        dialog.setContentText("Code:");
        dialog.showAndWait().ifPresentOrElse(
                entered -> verifyCode(persona, inbox, entered.trim()),
                inbox::close);
    }

    private void verifyCode(Persona persona, InboxWindow inbox, String code) {
        if (code.isEmpty()) {
            Dialogs.warn("Missing code", "Please enter the verification code.");
            promptForCode(persona, inbox);
            return;
        }
        setBusy(true);
        BackgroundJobs.run(
                () -> ir.ac.kntu.mail.MailService.verifyCode(persona.getEmail(), code),
                valid -> {
                    setBusy(false);
                    if (Boolean.TRUE.equals(valid)) {
                        inbox.close();
                        onAuthenticated(persona);
                    } else {
                        Dialogs.warn("Invalid code", "That code is incorrect or expired. Try again.");
                        promptForCode(persona, inbox);
                    }
                },
                error -> {
                    setBusy(false);
                    Dialogs.error("Verification failed", error);
                    promptForCode(persona, inbox);
                });
    }

    private void onAuthenticated(Persona persona) {
        if (persona == null) {
            Dialogs.error(SIGN_IN_FAILED, "Account could not be loaded.");
            return;
        }
        SessionManager.createSession(persona);
        Persona.setCurrentUser(persona);

        // Apply the user's saved theme, if any.
        if (navigator != null) {
            navigator.setTheme(UiTheme.from(persona.getTheme()));
            navigator.switchTo(new ir.ac.kntu.gui.shell.AppShell(navigator, persona));
            // Background notification check at login (due-soon loans, ready reservations).
            ir.ac.kntu.gui.notification.NotificationChecker.checkAndNotify(persona);
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
