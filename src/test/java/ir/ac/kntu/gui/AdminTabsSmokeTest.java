package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;

import static org.testfx.api.FxAssert.verifyThat;

/**
 * Consolidated smoke test for all admin tabs. Signs in as admin ONCE, then
 * clicks through each admin panel and asserts representative labels are visible.
 * Replaces the four separate single-tab admin panel tests (Fines, SupportStaff,
 * SystemSettings, UserManagement), cutting the cold-start 2FA cost from 4× to 1×.
 */
@ExtendWith(ApplicationExtension.class)
public class AdminTabsSmokeTest extends ApplicationTest {

    private Navigator navigator;

    @Start
    public void start(Stage stage) {
        LoginView login = new LoginView();
        navigator = new Navigator(stage, login);
        login.attachNavigator(navigator);
        stage.show();
    }

    @BeforeEach
    void resetDb() {
        PersonaService.reset();
        Persona.setCurrentUser(null);
    }

    @Test
    public void adminCanAccessAllAdminTabs() {
        GuiTestSupport.signIn(this, "admin@system.local");

        // Dashboard (default)
        verifyThat("Dashboard", NodeMatchers.isVisible());

        // Library / Search
        clickOn("Library / Search");
        verifyThat("Library & Search", NodeMatchers.isVisible());

        // Item Management
        clickOn("Item Management");
        verifyThat("Item Management", NodeMatchers.isVisible());

        // User Management
        clickOn("User Management");
        verifyThat("User Management", NodeMatchers.isVisible());
        verifyThat("Search users…", NodeMatchers.isVisible());
        verifyThat("Toggle active", NodeMatchers.isVisible());

        // Callcenter (SupportStaffPanel)
        clickOn("Callcenter");
        verifyThat("Callcenter Management", NodeMatchers.isVisible());
        verifyThat("Create agent", NodeMatchers.isVisible());

        // Fines
        clickOn("Fines");
        verifyThat("Fines — Indebted Users", NodeMatchers.isVisible());
        verifyThat("Outstanding debt", NodeMatchers.isVisible());

        // Analytics
        clickOn("Analytics");
        verifyThat("Analytics", NodeMatchers.isVisible());
        verifyThat("Generate HTML report", NodeMatchers.isVisible());

        // System Settings
        clickOn("System Settings");
        verifyThat("System Settings", NodeMatchers.isVisible());
        verifyThat("Borrow days", NodeMatchers.isVisible());
        verifyThat("Fine rate", NodeMatchers.isVisible());
    }
}
