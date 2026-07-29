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

@ExtendWith(ApplicationExtension.class)
public class ShellNavigationTest extends ApplicationTest {

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
    public void testAdminShellHasAdminNavItems() {
        signInWithMasterKey("admin@system.local");
        verifyThat(".sidebar", NodeMatchers.isVisible());
        verifyThat("Dashboard", NodeMatchers.isVisible());
        verifyThat("Library / Search", NodeMatchers.isVisible());
        verifyThat("Item Management", NodeMatchers.isVisible());
        verifyThat("User Management", NodeMatchers.isVisible());
        verifyThat("Fines", NodeMatchers.isVisible());
        verifyThat("Analytics", NodeMatchers.isVisible());
        verifyThat("System Settings", NodeMatchers.isVisible());
    }

    @Test
    public void testAdminTopBarShowsIdentity() {
        signInWithMasterKey("admin@system.local");
        verifyThat("admin@system.local  ·  ADMIN", NodeMatchers.isVisible());
    }

    @Test
    public void testSidebarContentSwap() {
        signInWithMasterKey("admin@system.local");
        clickOn("Library / Search");
        verifyThat("Library & Search", NodeMatchers.isVisible());
        clickOn("Item Management");
        verifyThat("Item Management", NodeMatchers.isVisible());
    }

    @Test
    public void testLogoutReturnsToLogin() {
        signInWithMasterKey("admin@system.local");
        verifyThat(".sidebar", NodeMatchers.isVisible());
        clickOn("Sign out");
        verifyThat("#emailField", NodeMatchers.isVisible());
        verifyThat("#loginButton", NodeMatchers.isVisible());
    }

    private void signInWithMasterKey(String email) {
        clickOn("#emailField").write(email);
        clickOn("#passwordField").write("bid");
        clickOn("#loginButton");
    }
}
