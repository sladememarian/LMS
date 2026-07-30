package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.admin.UserManagementPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.AdminManagementService;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

@ExtendWith(ApplicationExtension.class)
public class UserManagementPanelTest extends ApplicationTest {

    private Navigator navigator;

    @Start
    public void start(Stage stage) {
        LoginView login = new LoginView();
        navigator = new Navigator(stage, login);
        login.attachNavigator(navigator);
        stage.show();
    }

    private void loginAsAdmin() {
        clickOn("#emailField").write("admin@system.local");
        clickOn("#passwordField").write("bid");
        clickOn("#loginButton");
    }

    @BeforeEach
    void setUp() {
        PersonaService.reset();
        Persona.setCurrentUser(null);
    }

    @Test
    public void testUserManagementShowsUserTable() {
        loginAsAdmin();
        clickOn("User Management");
        
        verifyThat("User Management", isVisible());
        verifyThat("Search users…", isVisible());
        verifyThat("Email", isVisible());
        verifyThat("Role", isVisible());
        verifyThat("Member ID", isVisible());
        verifyThat("Active", isVisible());
    }

    @Test
    public void testUserManagementHasActionButtons() {
        loginAsAdmin();
        clickOn("User Management");
        
        verifyThat("Toggle active", isVisible());
        verifyThat("Reset password", isVisible());
        verifyThat("Delete", isVisible());
    }

    @Test
    public void testUserSearchWorks() {
        loginAsAdmin();
        clickOn("User Management");
        
        clickOn("Search users…").write("admin");
        
        // Table should filter
        verifyThat("admin@system.local", isVisible());
    }
}