package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.support.SupportUserPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.util.PersonaRepository;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
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
public class SupportUserPanelTest extends ApplicationTest {

    private Navigator navigator;

    @Start
    public void start(Stage stage) {
        LoginView login = new LoginView();
        navigator = new Navigator(stage, login);
        login.attachNavigator(navigator);
        stage.show();
    }

    private void loginAsUser() {
        GuiTestSupport.signIn(this, "student1@system.local");
    }

    @BeforeEach
    void setUp() {
        PersonaService.reset();
        Persona.setCurrentUser(null);
        Persona student = new Persona("student1@system.local", "bid");
        student.updateRole(UserRole.STUDENT);
        PersonaService.addPersona(student);
        PersonaRepository.insertPersona(student);
    }

    @Test
    public void testSupportUserCreateTicketFormVisible() {
        loginAsUser();
        clickOn("Support");
        
        verifyThat("#sectionBox", isVisible());
        verifyThat("#titleField", isVisible());
        verifyThat("#descriptionArea", isVisible());
        verifyThat("Create ticket", isVisible());
    }

    @Test
    public void testSupportUserTicketTableVisible() {
        loginAsUser();
        clickOn("Support");
        
        verifyThat("Ticket", isVisible());
        verifyThat("Section", isVisible());
        verifyThat("Title", isVisible());
        verifyThat("Status", isVisible());
        verifyThat("Response", isVisible());
    }
}