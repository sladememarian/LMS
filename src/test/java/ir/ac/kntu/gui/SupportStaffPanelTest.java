package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.admin.SupportStaffPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.AdminManagementService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportSection;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
public class SupportStaffPanelTest extends ApplicationTest {

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
    public void testSupportStaffShowsAgentTable() {
        loginAsAdmin();
        clickOn("Support Staff");
        
        verifyThat("Support-Staff Management", isVisible());
        verifyThat("Email", isVisible());
        verifyThat("Assigned sections", isVisible());
    }

    @Test
    public void testSupportStaffCreateAgentFormVisible() {
        loginAsAdmin();
        clickOn("Support Staff");
        
        verifyThat("Agent email", isVisible());
        verifyThat("Temp password", isVisible());
        verifyThat("Create agent", isVisible());
    }

    @Test
    public void testSupportStaffSectionAssignmentCheckboxesVisible() {
        loginAsAdmin();
        clickOn("Support Staff");
        
        // Verify the assign sections heading and buttons
        verifyThat("Assign sections to selected agent:", isVisible());
        verifyThat("Apply sections", isVisible());
        
        // Find checkboxes by CSS class
        assertNotNull(lookup(".check-box").query());
    }
}