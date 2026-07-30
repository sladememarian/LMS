package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.admin.FinesPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.finance.FinanceService;
import javafx.scene.control.TableView;
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
public class FinesPanelTest extends ApplicationTest {

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
    void resetDb() {
        PersonaService.reset();
    }

    @Test
    public void testFinesShowsDebtorTable() {
        loginAsAdmin();
        clickOn("Fines");
        
        verifyThat("Fines — Indebted Users", isVisible());
        verifyThat("Email", isVisible());
        verifyThat("Member ID", isVisible());
        verifyThat("Outstanding debt", isVisible());
    }

    @Test
    public void testFinesTableShowsOnlyIndebtedUsers() {
        loginAsAdmin();
        clickOn("Fines");
        
        // Only users with debt > 0 should appear
        verifyThat("No indebted users.", isVisible());
    }
}