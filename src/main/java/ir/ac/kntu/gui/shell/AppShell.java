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
import ir.ac.kntu.gui.view.profile.ProfilePanel;
import ir.ac.kntu.gui.view.wallet.WalletPanel;
import ir.ac.kntu.gui.view.wallet.AdminWalletPanel;
import ir.ac.kntu.gui.view.support.SupportUserPanel;
import ir.ac.kntu.gui.view.support.SupportInboxPanel;
import ir.ac.kntu.gui.view.admin.UserManagementPanel;
import ir.ac.kntu.gui.view.admin.SupportStaffPanel;
import ir.ac.kntu.gui.view.admin.SystemSettingsPanel;
import ir.ac.kntu.gui.view.admin.FinesPanel;
import ir.ac.kntu.gui.view.admin.AnalyticsPanel;
import ir.ac.kntu.gui.notification.NotificationsPanel;
import ir.ac.kntu.gui.notification.NotificationChecker;
import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.sso.SessionManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

// The main post-login shell: a top bar (identity, theme, logout), a left
// sidebar whose entries depend on the user's role, and a central content area
// that is swapped as the user navigates (multi-scene requirement). Menu sets
// are assembled per role in buildNavItems().
public class AppShell implements View {

    private static final String DASHBOARD = "Dashboard";
    private static final String GHOST_STYLE = "ghost";

    private final Navigator navigator;
    private final Persona persona;
    private final BorderPane root = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final ScrollPane contentScroll = new ScrollPane();
    private final ToggleGroup navGroup = new ToggleGroup();

    // A tiny red dot shown beside "Notifications" when unread mail exists.
    private final Circle unreadDot = new Circle(4);

    // Next-Day control + its busy spinner (admin top bar); locked while running.
    private final Button nextDayButton = new Button("Next Day");
    private final ProgressIndicator nextDaySpinner = new ProgressIndicator();

    public AppShell(Navigator navigator, Persona persona) {
        this.navigator = navigator;
        this.persona = persona;
        build();
    }

    private void build() {
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        contentArea.getStyleClass().add("content-area");
        // Wrap the content in a scroll pane so tall panels (e.g. Profile) can be
        // scrolled when they exceed the viewport height.
        contentScroll.setContent(contentArea);
        contentScroll.setFitToWidth(true);
        contentScroll.setFitToHeight(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.getStyleClass().add("content-scroll");
        root.setCenter(contentScroll);
        refreshUnreadIndicator();
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
        themeToggle.getStyleClass().add(GHOST_STYLE);
        themeToggle.setOnAction(event -> {
            navigator.toggleTheme();
            // Bonus: persist the chosen theme so it is restored on next login.
            String themeKey = navigator.getTheme().key();
            BackgroundJobs.runAction(
                    () -> ir.ac.kntu.sso.SsoService.changeTheme(persona.getEmail(), themeKey),
                    null,
                    error -> { /* non-critical */ });
        });

        Button logout = new Button("Sign out");
        logout.getStyleClass().add(GHOST_STYLE);
        logout.setOnAction(event -> handleLogout());

        HBox bar = new HBox(14, appName, spacer, identity);
        if (persona.getRole() == UserRole.ADMIN) {
            nextDayButton.getStyleClass().add(GHOST_STYLE);
            nextDayButton.setOnAction(event -> handleNextDay());
            nextDaySpinner.setMaxSize(18, 18);
            nextDaySpinner.setVisible(false);
            nextDaySpinner.setManaged(false);
            bar.getChildren().addAll(nextDayButton, nextDaySpinner);
        }
        bar.getChildren().addAll(themeToggle, logout);
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
        // Control-flow loop: builds a toggle button per item with side effects on
        // the sidebar and a stateful "select the first one" flag — not a value
        // transformation, so it stays imperative.
        boolean first = true;
        for (NavItem item : items) {
            ToggleButton button = new ToggleButton(item.label());
            button.setToggleGroup(navGroup);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().add("nav-item");
            boolean isNotifications = "Notifications".equals(item.label());
            if (isNotifications) {
                button.setGraphic(buildUnreadDotGraphic());
                button.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            }
            button.setOnAction(event -> {
                button.setSelected(true);
                showContent(item.buildContent());
                if (isNotifications) {
                    // Opening the tab counts as reading the latest; refresh the dot.
                    refreshUnreadIndicator();
                }
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

    // Builds the unread-notification dot graphic for the Notifications button.
    private HBox buildUnreadDotGraphic() {
        unreadDot.getStyleClass().add("unread-dot");
        unreadDot.setManaged(false);
        unreadDot.setVisible(false);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox graphic = new HBox(6, gap, unreadDot);
        graphic.setAlignment(Pos.CENTER_RIGHT);
        graphic.setMaxWidth(Double.MAX_VALUE);
        graphic.setMinWidth(120);
        return graphic;
    }

    private void showContent(Node node) {
        contentArea.getChildren().setAll(node);
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    // Assembles the sidebar based on the user's role.
    private List<NavItem> buildNavItems() {
        UserRole role = persona.getRole();
        List<NavItem> items = new ArrayList<>();

        if (role == UserRole.ADMIN) {
            items.add(new NavItem(DASHBOARD, () -> new DashboardPanel(persona)));
            items.add(new NavItem("Library / Search", LibrarySearchPanel::new));
            items.add(new NavItem("Item Management", ItemManagementPanel::new));
            items.add(new NavItem("User Management", () -> new UserManagementPanel(persona)));
            items.add(new NavItem("Callcenter", () -> new SupportStaffPanel(persona)));
            items.add(new NavItem("Fines", FinesPanel::new));
            items.add(new NavItem("Wallet", () -> new AdminWalletPanel(persona)));
            items.add(new NavItem("Analytics", AnalyticsPanel::new));
            items.add(new NavItem("System Settings", () -> new SystemSettingsPanel(persona)));
        } else if (role == UserRole.CALLCENTER) {
            items.add(new NavItem(DASHBOARD, () -> new DashboardPanel(persona)));
            items.add(new NavItem("Support Inbox", () -> new SupportInboxPanel(persona)));
            items.add(new NavItem("Item Management", ItemManagementPanel::new));
            items.add(new NavItem("Fines", FinesPanel::new));
        } else {
            // Regular users: STUDENT, TEACHER, GUEST
            items.add(new NavItem(DASHBOARD, () -> new DashboardPanel(persona)));
            items.add(new NavItem("Library / Search", LibrarySearchPanel::new));
            items.add(new NavItem("Loans & Reservations", () -> new LoansReservationsPanel(persona)));
            items.add(new NavItem("Wallet", () -> new WalletPanel(persona)));
            items.add(new NavItem("Support", () -> new SupportUserPanel(persona)));
        }

        // Shared for every role:
        items.add(new NavItem("Notifications",
                () -> new NotificationsPanel(persona, this::refreshUnreadIndicator)));
        items.add(new NavItem("Profile", () -> new ProfilePanel(persona, navigator)));
        return items;
    }

    // Recomputes whether the user has any unread mail (off the FX thread) and
    // shows/hides the little red dot beside the Notifications nav item.
    private void refreshUnreadIndicator() {
        String email = persona.getEmail();
        if (email == null) {
            return;
        }
        BackgroundJobs.run(
                () -> {
                    Inbox inbox = MailService.getInbox(email);
                    if (inbox == null) {
                        return Boolean.FALSE;
                    }
                    return inbox.getMessages().stream().anyMatch(m -> !m.isRead());
                },
                hasUnread -> {
                    boolean unread = Boolean.TRUE.equals(hasUnread);
                    unreadDot.setVisible(unread);
                    unreadDot.setManaged(unread);
                },
                error -> { /* non-critical: indicator only */ });
    }

    // Advances the simulated day, accrues overdue debts, expires reservations,
    // then re-checks notifications — all on a background thread. The button is
    // locked and a spinner shown while it runs so the slow per-row DB writes
    // can't be re-triggered or appear frozen.
    private void handleNextDay() {
        nextDayButton.setDisable(true);
        nextDaySpinner.setVisible(true);
        nextDaySpinner.setManaged(true);
        BackgroundJobs.run(
                () -> {
                    int day = SimulationClock.advanceDay();
                    LoanService.accrueOverdueDebts(day);
                    ReservationService.expireReservations(day);
                    return day;
                },
                day -> {
                    nextDayButton.setDisable(false);
                    nextDaySpinner.setVisible(false);
                    nextDaySpinner.setManaged(false);
                    ir.ac.kntu.gui.util.Dialogs.info("Day advanced",
                            "Simulated day is now " + day + ".");
                    NotificationChecker.checkAndNotify(persona);
                    refreshUnreadIndicator();
                },
                error -> {
                    nextDayButton.setDisable(false);
                    nextDaySpinner.setVisible(false);
                    nextDaySpinner.setManaged(false);
                    ir.ac.kntu.gui.util.Dialogs.error("Could not advance day", error);
                });
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
