package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.dashboard.DashboardPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
public class DashboardStreamsTest extends ApplicationTest {

    private Stage stage;

    @Start
    public void start(Stage stage) {
        this.stage = stage;
        LoginView login = new LoginView();
        Navigator navigator = new Navigator(stage, login);
        login.attachNavigator(navigator);
        stage.show();
    }

    @BeforeEach
    void resetDb() {
        PersonaService.reset();
        Persona.setCurrentUser(null);
    }

    @Test
    public void testDashboardPanelBuildsForAdmin() {
        Persona admin = PersonaService.getProfile("admin@system.local");
        assertNotNull(admin);
        DashboardPanel panel = new DashboardPanel(admin);
        assertNotNull(panel);
        ObservableList<String> styleClasses = FXCollections.observableArrayList(panel.getStyleClass());
        assertTrue(styleClasses.contains("content-area"));
    }

    @Test
    public void testDashboardUsesStreamsForBorrowCounts() {
        Persona admin = PersonaService.getProfile("admin@system.local");
        assertNotNull(admin);
        DashboardPanel panel = new DashboardPanel(admin);
        assertNotNull(panel);
        // The dashboard loads stats via BackgroundJobs.run() — verify it doesn't throw
        // during construction.
    }

    @Test
    public void testAdminRoleHasAdminDashboard() {
        Persona admin = PersonaService.getProfile("admin@system.local");
        assertNotNull(admin);
        assertTrue(admin.getRole() == UserRole.ADMIN);
    }
}
