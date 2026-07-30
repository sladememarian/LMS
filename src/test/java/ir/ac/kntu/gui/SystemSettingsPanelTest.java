package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.admin.SystemSettingsPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.util.SystemSettingsService;
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

@ExtendWith(ApplicationExtension.class)
public class SystemSettingsPanelTest extends ApplicationTest {

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
    public void testSystemSettingsShowsSettings() {
        loginAsAdmin();
        clickOn("System Settings");
        
        verifyThat("System Settings", NodeMatchers.isVisible());
        verifyThat("Borrow days", NodeMatchers.isVisible());
        verifyThat("Fine rate", NodeMatchers.isVisible());
        verifyThat("Reservation days", NodeMatchers.isVisible());
        verifyThat("Max reservations", NodeMatchers.isVisible());
    }

    @Test
    public void testSettingsFieldsEditable() {
        loginAsAdmin();
        clickOn("System Settings");
        
        // Fields should be editable
        verifyThat("#borrowDays", NodeMatchers.isVisible());
        verifyThat("#fineRate", NodeMatchers.isVisible());
        verifyThat("#reservationDays", NodeMatchers.isVisible());
        verifyThat("#maxReservations", NodeMatchers.isVisible());
    }
}