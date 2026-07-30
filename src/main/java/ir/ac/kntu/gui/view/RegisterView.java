package ir.ac.kntu.gui.view;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.View;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.Validator;
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
 * Registration screen. Creates a new persona via the existing
 * {@link PersonaService} on a background thread and reports validation/duplicate
 * errors through {@link Dialogs}.
 */
public class RegisterView implements View {

    private static final String FIELD_STYLE = "field";

    private final Navigator navigator;
    private final StackPane root = new StackPane();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final Button submitButton = new Button("Create account");
    private final ProgressIndicator spinner = new ProgressIndicator();

    public RegisterView(Navigator navigator) {
        this.navigator = navigator;
        build();
    }

    private void build() {
        Label heading = new Label("Create account");
        heading.getStyleClass().add("h1");
        Label subtitle = new Label("Register as a new library member");
        subtitle.getStyleClass().add("muted");

        emailField.setPromptText("Email");
        passwordField.setPromptText("Password");
        confirmField.setPromptText("Confirm password");
        emailField.getStyleClass().add(FIELD_STYLE);
        passwordField.getStyleClass().add(FIELD_STYLE);
        confirmField.getStyleClass().add(FIELD_STYLE);

        submitButton.getStyleClass().add("primary");
        submitButton.setDefaultButton(true);
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setOnAction(event -> handleRegister());

        Hyperlink backToLogin = new Hyperlink("Back to sign in");
        backToLogin.setOnAction(event -> goToLogin());

        spinner.setMaxSize(22, 22);
        spinner.setVisible(false);
        HBox buttonRow = new HBox(10, submitButton, spinner);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, heading, subtitle, emailField, passwordField,
                confirmField, buttonRow, backToLogin);
        card.getStyleClass().add("card");
        card.setMaxWidth(360);
        card.setPadding(new Insets(28));

        root.getChildren().add(card);
        root.getStyleClass().add("auth-bg");
        StackPane.setAlignment(card, Pos.CENTER);
    }

    private void handleRegister() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmField.getText() == null ? "" : confirmField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            Dialogs.warn("Missing information", "Email and password are required.");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            Dialogs.warn("Invalid email", "Please enter a valid email address.");
            return;
        }
        if (!Validator.isValidPassword(password)) {
            Dialogs.warn("Weak password",
                    "Password must be at least 8 characters with uppercase, lowercase, digit, and special character.");
            return;
        }
        if (!password.equals(confirm)) {
            Dialogs.warn("Passwords do not match", "Please re-enter the same password.");
            return;
        }

        setBusy(true);
        BackgroundJobs.runAction(
                () -> PersonaService.registerPersona(email, password),
                () -> {
                    setBusy(false);
                    Dialogs.info("Account created",
                            "You can now sign in with your new account.");
                    goToLogin();
                },
                error -> {
                    setBusy(false);
                    Dialogs.error("Registration failed", error);
                });
    }

    private void goToLogin() {
        LoginView login = new LoginView();
        login.attachNavigator(navigator);
        navigator.switchTo(login);
    }

    private void setBusy(boolean busy) {
        spinner.setVisible(busy);
        submitButton.setDisable(busy);
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
        confirmField.setDisable(busy);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public String title() {
        return "Register";
    }
}
