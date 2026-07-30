package ir.ac.kntu.gui.shell;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.View;
import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.dashboard.DashboardPanel;
import ir.ac.kntu.gui.view.library.LibrarySearchPanel;
import ir.ac.kntu.gui.view.library.ItemManagementPanel;
import ir.ac.kntu.gui.view.loans.LoansReservationsPanel;
import ir.ac.kntu.gui.view.wallet.WalletPanel;
import ir.ac.kntu.gui.view.support.SupportUserPanel;
import ir.ac.kntu.gui.view.support.SupportInboxPanel;
import ir.ac.kntu.gui.view.admin.UserManagementPanel;
import ir.ac.kntu.gui.view.admin.SupportStaffPanel;
import ir.ac.kntu.gui.view.admin.SystemSettingsPanel;
import ir.ac.kntu.gui.view.admin.FinesPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.sso.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The main post-login application shell: a top bar (identity, theme, logout), a
 * left sidebar whose entries depend on the signed-in user's role, and a central
 * content area that is swapped as the user navigates (multi-scene requirement).
 *
 * <p>Menu sets are assembled per role in {@link #buildNavItems()}. Screens are
 * currently placeholders and are filled in by later migration steps.
 */
public class AppShell implements View {

    private static final String DASHBOARD = "Dashboard";
    private static final String ITEM_MGMT = "Item Management";
    private static final String FINES = "Fines";

    private final Navigator navigator;
    private final Persona persona;
    private final BorderPane root = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final ToggleGroup navGroup = new ToggleGroup();

    public AppShell(Navigator navigator, Persona persona) {
        this.navigator = navigator;
        this.persona = persona;
        build();
    }

    private void build() {
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        contentArea.getStyleClass().add("content-area");
        root.setCenter(contentArea);
    }

    private Node buildTopBar() {
        Label appName = new Label("KNTU Library");
        appName.getStyleClass().add("brand");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String who = persona.getEmail() != null ? persona.getEmail() : persona.getUsername();
        Label identity = new Label(who + "  ·  " + persona.getRole());
        identity.getStyleClass().add("muted");

        Button themeToggle = new Button("Theme");
        themeToggle.getStyleClass().add("ghost");
        themeToggle.setOnAction(event -> navigator.toggleTheme());

        Button logout = new Button("Sign out");
        logout.getStyleClass().add("ghost");
        logout.setOnAction(event -> handleLogout());

        HBox bar = new HBox(14, appName, spacer, identity, themeToggle, logout);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.getStyleClass().add("topbar");
        return bar;
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);

        List<NavItem> items = buildNavItems();
        boolean first = true;
        for (NavItem item : items) {
            ToggleButton button = new ToggleButton(item.label());
            button.setToggleGroup(navGroup);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().add("nav-item");
            button.setOnAction(event -> {
                button.setSelected(true);
                showContent(item.buildContent());
            });
            sidebar.getChildren().add(button);
            if (first) {
                button.setSelected(true);
                showContent(item.buildContent());
                first = false;
            }
        }
        return sidebar;
    }

    private void showContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    /** Assembles the sidebar based on the user's role. */
    private List<NavItem> buildNavItems() {
        UserRole role = persona.getRole();
        List<NavItem> items = new ArrayList<>();

        if (role == UserRole.ADMIN) {
            items.add(new NavItem(DASHBOARD, () -> new DashboardPanel(persona)));
            items.add(new NavItem("Library / Search", LibrarySearchPanel::new));
            items.add(new NavItem(ITEM_MGMT, ItemManagementPanel::new));
            items.add(new NavItem("User Management", () -> new UserManagementPanel(persona)));
            items.add(new NavItem("Support Staff", () -> new SupportStaffPanel(persona)));
            items.add(new NavItem(FINES, FinesPanel::new));
            items.add(new NavItem("Analytics", () -> Placeholder.build("Analytics",
                    "Top-10 borrowed items (BarChart) and monthly fine revenue (LineChart).")));
            items.add(new NavItem("System Settings", () -> new SystemSettingsPanel(persona)));
        } else if (role == UserRole.CALLCENTER) {
            items.add(new NavItem(DASHBOARD, () -> new DashboardPanel(persona)));
            items.add(new NavItem("Support Inbox", () -> new SupportInboxPanel(persona)));
            items.add(new NavItem(ITEM_MGMT, ItemManagementPanel::new));
            items.add(new NavItem(FINES, FinesPanel::new));
        } else {
            // Regular users: STUDENT, TEACHER, GUEST
            items.add(new NavItem(DASHBOARD, () -> new DashboardPanel(persona)));
            items.add(new NavItem("Library / Search", LibrarySearchPanel::new));
            items.add(new NavItem("Loans & Reservations", () -> new LoansReservationsPanel(persona)));
            items.add(new NavItem("Wallet", () -> new WalletPanel(persona)));
            items.add(new NavItem("Support", () -> new SupportUserPanel(persona)));
        }

        // Shared for every role:
        items.add(new NavItem("Notifications", () -> Placeholder.build("Notifications",
                "Inbox of read/unread notifications; due-soon and reservation-active alerts "
                + "are checked on a background thread at login and on 'Next Day'.")));
        items.add(new NavItem("Profile", () -> Placeholder.build("Profile",
                "View and edit your profile, change password and toggle theme.")));
        return items;
    }

    private void handleLogout() {
        SessionManager.destroySession();
        Persona.setCurrentUser(null);
        LoginView login = new LoginView();
        login.attachNavigator(navigator);
        navigator.switchTo(login);
    }

    @Override
    public javafx.scene.Parent getRoot() {
        return root;
    }

    @Override
    public String title() {
        return persona.getRole() + " workspace";
    }
}
