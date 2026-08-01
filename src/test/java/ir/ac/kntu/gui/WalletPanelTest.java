package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.wallet.WalletPanel;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.util.PersonaRepository;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
public class WalletPanelTest extends ApplicationTest {

    private Navigator navigator;

    @Start
    public void start(Stage stage) {
        LoginView login = new LoginView();
        navigator = new Navigator(stage, login);
        login.attachNavigator(navigator);
        stage.show();
    }

    private void loginAsStudent() {
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
    public void testWalletShowsBalanceAndDebt() {
        loginAsStudent();
        clickOn("Wallet");
        
        // Verify stat cards visible
        verifyThat("Balance", isVisible());
        verifyThat("Outstanding debt", isVisible());
    }

    @Test
    public void testWalletHasTopUpAndPayDebtButtons() {
        loginAsStudent();
        clickOn("Wallet");
        
        verifyThat("Top up", isVisible());
        verifyThat("Pay debt", isVisible());
    }

    @Test
    public void testTransactionHistoryTableVisible() {
        loginAsStudent();
        clickOn("Wallet");
        
        verifyThat("Transaction history", isVisible());
        verifyThat("Date", isVisible());
        verifyThat("Type", isVisible());
        verifyThat("Amount", isVisible());
        verifyThat("Description", isVisible());
    }
}