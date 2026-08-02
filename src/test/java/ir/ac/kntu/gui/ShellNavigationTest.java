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

    /**
     * One admin session that walks the whole shell: sign in once, assert the
     * nav items and identity, swap the sidebar content, then sign out once and
     * confirm the login screen returns — instead of re-running the full 2FA
     * sign-in for each of these checks.
     */
    @Test
    public void testAdminShellNavigationEndToEnd() {
        signInWithMasterKey("admin@system.local");

        // Nav items present for an admin.
        verifyThat(".sidebar", NodeMatchers.isVisible());
        verifyThat("Dashboard", NodeMatchers.isVisible());
        verifyThat("Library / Search", NodeMatchers.isVisible());
        verifyThat("Item Management", NodeMatchers.isVisible());
        verifyThat("User Management", NodeMatchers.isVisible());
        verifyThat("Fines", NodeMatchers.isVisible());
        verifyThat("Analytics", NodeMatchers.isVisible());
        verifyThat("System Settings", NodeMatchers.isVisible());

        // Top bar identity.
        verifyThat("admin@system.local  ·  ADMIN", NodeMatchers.isVisible());

        // Sidebar content swaps as the user navigates.
        clickOn("Library / Search");
        verifyThat("Library & Search", NodeMatchers.isVisible());
        clickOn("Item Management");
        verifyThat("Item Management", NodeMatchers.isVisible());

        // Sign out once, back to login.
        clickOn("Sign out");
        verifyThat("#emailField", NodeMatchers.isVisible());
        verifyThat("#loginButton", NodeMatchers.isVisible());
    }

    private void signInWithMasterKey(String email) {
        GuiTestSupport.signIn(this, email);
    }
}
