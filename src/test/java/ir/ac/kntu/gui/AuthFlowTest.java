package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
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
public class AuthFlowTest extends ApplicationTest {

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
    public void testLoginElementsExist() {
        verifyThat("#emailField", (TextField tf) -> !tf.isDisabled());
        verifyThat("#passwordField", (PasswordField pf) -> !pf.isDisabled());
        verifyThat("#loginButton", (Button b) -> !b.isDisabled());
    }

    @Test
    public void testEmptyLoginStaysOnLoginScreen() {
        clickOn("#loginButton");
        // Fields should remain enabled (no navigation happened)
        verifyThat("#emailField", NodeMatchers.isVisible());
        verifyThat("#passwordField", NodeMatchers.isVisible());
        verifyThat("#loginButton", NodeMatchers.isVisible());
    }

    @Test
    public void testInvalidCredentialsStaysOnLoginScreen() {
        clickOn("#emailField").write("nobody@example.com");
        clickOn("#passwordField").write("wrongpass");
        clickOn("#loginButton");
        // Should still be on login screen
        verifyThat("#emailField", NodeMatchers.isVisible());
        verifyThat("#loginButton", NodeMatchers.isVisible());
    }

    @Test
    public void testMasterKeyLoginNavigatesToHome() {
        // Full flow: master-key credentials, then the 2FA dialog answered with
        // the master OTP. Only after verification does the AppShell appear.
        GuiTestSupport.signIn(this, "admin@system.local");
        verifyThat(".sidebar", NodeMatchers.isVisible());
    }
}
