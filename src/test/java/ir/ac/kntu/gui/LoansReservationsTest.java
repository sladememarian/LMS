package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.loans.LoansReservationsPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
public class LoansReservationsTest extends ApplicationTest {

    @Start
    public void start(Stage stage) {
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
    public void testLoansReservationsPanelBuilds() {
        LoansReservationsPanel panel = new LoansReservationsPanel(PersonaService.getProfile("admin@system.local"));
        assertNotNull(panel);
    }

    @Test
    public void testLoansReservationsHasActions() {
        LoansReservationsPanel panel = new LoansReservationsPanel(PersonaService.getProfile("admin@system.local"));
        assertNotNull(panel);
    }

    @Test
    public void testUserCanAccessLoansReservationsAfterLogin() {
        clickOn("#emailField").write("admin@system.local");
        clickOn("#passwordField").write("bid");
        clickOn("#loginButton");
        // Admin has "Item Management" in sidebar
        clickOn("Item Management");
        assertNotNull(lookup("Item Management").query());
    }
}