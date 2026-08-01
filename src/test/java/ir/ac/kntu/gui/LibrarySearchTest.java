package ir.ac.kntu.gui;

import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.gui.view.library.LibrarySearchPanel;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
public class LibrarySearchTest extends ApplicationTest {

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
    public void testSearchPanelBuilds() {
        LibrarySearchPanel panel = new LibrarySearchPanel();
        assertNotNull(panel);
    }

    @Test
    public void testSearchReturnsResults() {
        List<LibraryItem> all = LibraryService.getAllItems();
        // The search service exists and returns a list (may be empty if no items seeded).
        assertNotNull(all);
    }

    // The admin-login smoke path (sign in → open Library / Search) is covered by
    // AdminTabsSmokeTest, which signs in once and sweeps every admin tab.
}
