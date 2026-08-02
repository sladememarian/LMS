package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.support.SupportInboxPanel;
import ir.ac.kntu.gui.view.support.SupportUserPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.SupportSection;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
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
public class SupportInboxPanelTest extends ApplicationTest {

    private Navigator navigator;

    @Start
    public void start(Stage stage) {
        LoginView login = new LoginView();
        navigator = new Navigator(stage, login);
        login.attachNavigator(navigator);
        stage.show();
    }

    @BeforeEach
    void setUp() {
        PersonaService.reset();
        Persona.setCurrentUser(null);
    }

    private void loginAsCallCenter() {
        GuiTestSupport.signIn(this, "callcenter@system.local");
    }

    /**
     * Single call-center session over the Support Inbox: sign in once, open the
     * inbox, assert the ticket table columns and the action buttons are all
     * present, then sign out once — instead of re-running the full 2FA sign-in
     * for each assertion group.
     */
    @Test
    public void testSupportInboxEndToEnd() {
        loginAsCallCenter();
        clickOn("Support Inbox");

        // Ticket table columns.
        verifyThat("Ticket", isVisible());
        verifyThat("User", isVisible());
        verifyThat("Section", isVisible());
        verifyThat("Title", isVisible());
        verifyThat("Priority", isVisible());
        verifyThat("Status", isVisible());

        // Action buttons.
        verifyThat("Reply", isVisible());
        verifyThat("Close ticket", isVisible());
        verifyThat("Refresh", isVisible());

        GuiTestSupport.signOut(this);
    }
}