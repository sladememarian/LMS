package ir.ac.kntu.gui.view;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.View;
import ir.ac.kntu.gui.component.PasswordBox;
import ir.ac.kntu.gui.signup.SignupEnvelope;
import ir.ac.kntu.gui.signup.SignupLog;
import ir.ac.kntu.gui.signup.SignupService;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.util.Validator;
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

// Registration screen. Collects email, first name, last name, phone, password
// and confirm. The account is created fast (email + password only) via
// SignupService so the user can sign in immediately; the profile fields are
// queued and persisted by a background worker thread.
public class RegisterView implements View {

    private static final String FIELD_STYLE = "field";

    private final Navigator navigator;
    private final StackPane root = new StackPane();
    private final TextField emailField = new TextField();
    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField phoneField = new TextField();
    private final PasswordBox passwordField = new PasswordBox();
    private final PasswordBox confirmField = new PasswordBox();
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

        configureFields();

        submitButton.getStyleClass().add("primary");
        submitButton.setId("registerButton");
        submitButton.setDefaultButton(true);
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setOnAction(event -> handleRegister());

        Hyperlink backToLogin = new Hyperlink("Back to sign in");
        backToLogin.setOnAction(event -> goToLogin());

        spinner.setMaxSize(22, 22);
        spinner.setVisible(false);
        HBox buttonRow = new HBox(10, submitButton, spinner);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, heading, subtitle, emailField, firstNameField,
                lastNameField, phoneField, passwordField, confirmField,
                buttonRow, backToLogin);
        card.getStyleClass().add("card");
        card.setMaxWidth(360);
        card.setPadding(new Insets(28));

        root.getChildren().add(card);
        root.getStyleClass().add("auth-bg");
        StackPane.setAlignment(card, Pos.CENTER);
    }

    // Prompt text, fx:ids, and style classes for every input field.
    private void configureFields() {
        configureText(emailField, "Email", "emailField");
        configureText(firstNameField, "First name", "firstNameField");
        configureText(lastNameField, "Last name", "lastNameField");
        configureText(phoneField, "Phone (+98/98/0)", "phoneField");
        passwordField.setPromptText("Password");
        passwordField.setFieldId("passwordField");
        confirmField.setPromptText("Confirm password");
        confirmField.setFieldId("confirmField");
    }

    private void configureText(TextField field, String prompt, String id) {
        field.setPromptText(prompt);
        field.setId(id);
        field.getStyleClass().add(FIELD_STYLE);
    }

    private void handleRegister() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String first = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String last = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmField.getText() == null ? "" : confirmField.getText();

        SignupEnvelope profile = new SignupEnvelope(email, first, last, phone);
        if (!isValid(profile, password, confirm)) {
            return;
        }

        setBusy(true);
        SignupService.submit(
                profile,
                password,
                () -> {
                    setBusy(false);
                    SignupLog.step(SignupLog.THREAD_B, "Account created window box opened");
                    Dialogs.info("Account created",
                            "You can sign in now. Your profile details are being saved in the background.");
                    goToLogin();
                },
                error -> {
                    setBusy(false);
                    Dialogs.error("Registration failed", error);
                });
    }

    // Validates all registration fields, showing a warning for the first problem.
    private boolean isValid(SignupEnvelope profile, String password, String confirm) {
        String email = profile.getEmail();
        if (email.isEmpty() || password.isEmpty()) {
            Dialogs.warn("Missing information", "Email and password are required.");
            return false;
        }
        if (Validator.isBlank(profile.getFirstName()) || Validator.isBlank(profile.getLastName())) {
            Dialogs.warn("Missing information", "First and last name are required.");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            Dialogs.warn("Invalid email", "Please enter a valid email address.");
            return false;
        }
        if (!Validator.isValidPhoneNumber(profile.getPhoneNumber())) {
            Dialogs.warn("Invalid phone",
                    "Phone must match (+98/98/0) followed by 9 digits.");
            return false;
        }
        if (!Validator.isValidPassword(password)) {
            Dialogs.warn("Weak password",
                    "Password must be at least 8 characters with uppercase, lowercase, digit, and special character.");
            return false;
        }
        if (!password.equals(confirm)) {
            Dialogs.warn("Passwords do not match", "Please re-enter the same password.");
            return false;
        }
        return true;
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
        firstNameField.setDisable(busy);
        lastNameField.setDisable(busy);
        phoneField.setDisable(busy);
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
