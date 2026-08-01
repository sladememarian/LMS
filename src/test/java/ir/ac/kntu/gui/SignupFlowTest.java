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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testfx.api.FxAssert.verifyThat;

/**
 * End-to-end test for the async signup flow: register a new member with
 * first/last/phone, confirm the account is created immediately (so login works),
 * then verify the profile fields land shortly after via the background worker.
 */
@ExtendWith(ApplicationExtension.class)
public class SignupFlowTest extends ApplicationTest {

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
    public void testAsyncSignupCreatesAccountAndQueuesProfile() throws InterruptedException {
        String email = "newmember@test.local";
        String password = "Secure@123";
        String firstName = "Alice";
        String lastName = "Lovelace";
        String phone = "09123456789";

        // Navigate to register screen via the login hyperlink
        clickOn("Create a new account");
        verifyThat("#registerButton", NodeMatchers.isVisible());

        // Fill all fields
        clickOn("#emailField").write(email);
        clickOn("#firstNameField").write(firstName);
        clickOn("#lastNameField").write(lastName);
        clickOn("#phoneField").write(phone);
        clickOn("#passwordField").write(password);
        clickOn("#confirmField").write(password);

        // Submit
        clickOn("#registerButton");

        // The success dialog appears quickly (account ready, profile queued)
        GuiTestSupport.waitForText(this, "Account created");
        clickOn("OK");

        // The account exists immediately — sign in with master key
        GuiTestSupport.signIn(this, email);
        verifyThat(".sidebar", NodeMatchers.isVisible());

        // The background worker should persist the profile within a few seconds.
        // Poll the backend directly (no GUI dependency).
        Persona profile = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            profile = PersonaService.getProfile(email);
            if (profile != null
                    && firstName.equals(profile.getFirstName())
                    && lastName.equals(profile.getLastName())
                    && phone.equals(profile.getPhoneNumber())) {
                break;
            }
            Thread.sleep(150);
        }

        assertNotNull(profile, "Profile not found after signup");
        assertEquals(firstName, profile.getFirstName());
        assertEquals(lastName, profile.getLastName());
        assertEquals(phone, profile.getPhoneNumber());
    }
}
