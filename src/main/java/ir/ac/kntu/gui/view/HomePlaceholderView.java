package ir.ac.kntu.gui.view;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.View;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.sso.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Temporary landing screen shown after a successful login. It confirms the
 * end-to-end flow works (login -> session -> routed screen). Step 2 replaces
 * this with the real role-based application shell and dashboards.
 */
public class HomePlaceholderView implements View {

    private final Navigator navigator;
    private final Persona persona;
    private final VBox root = new VBox(16);

    public HomePlaceholderView(Navigator navigator, Persona persona) {
        this.navigator = navigator;
        this.persona = persona;
        build();
    }

    private void build() {
        String who = persona.getEmail() != null ? persona.getEmail() : persona.getUsername();

        Label welcome = new Label("Welcome, " + who);
        welcome.getStyleClass().add("h1");

        Label roleLabel = new Label("Role: " + persona.getRole());
        roleLabel.getStyleClass().add("muted");

        Label note = new Label("Foundation ready. The role-based shell, dashboards, "
                + "library search and admin screens arrive in the next steps.");
        note.setWrapText(true);
        note.getStyleClass().add("muted");
        note.setMaxWidth(520);

        Button themeToggle = new Button("Toggle theme");
        themeToggle.getStyleClass().add("ghost");
        themeToggle.setOnAction(event -> navigator.toggleTheme());

        Button logout = new Button("Sign out");
        logout.getStyleClass().add("primary");
        logout.setOnAction(event -> handleLogout());

        VBox card = new VBox(14, welcome, roleLabel, note, themeToggle, logout);
        card.getStyleClass().add("card");
        card.setMaxWidth(560);
        card.setPadding(new Insets(28));

        root.getChildren().add(card);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("auth-bg");
        root.setPadding(new Insets(24));
    }

    private void handleLogout() {
        SessionManager.destroySession();
        Persona.setCurrentUser(null);
        LoginView login = new LoginView();
        login.attachNavigator(navigator);
        navigator.switchTo(login);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public String title() {
        return "Home";
    }
}
