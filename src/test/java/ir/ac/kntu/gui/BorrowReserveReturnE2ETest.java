package ir.ac.kntu.gui;

import java.util.concurrent.TimeUnit;

import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.gui.view.LoginView;
import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.reservation.Reservation;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.reservation.ReservationStatus;
import ir.ac.kntu.util.PersonaRepository;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real end-to-end flows (issue #6): instead of only asserting that widgets are
 * visible, these drive the GUI the way a user would — sign in through the 2FA
 * flow, click Borrow / Reserve / Return — and then assert that the backend
 * state actually changed (available copies, loans, reservation status). They
 * run headed and are excluded from CI via {@code -PskipGuiTests}.
 *
 * <p>Each test seeds its own single-copy {@link Book} through the existing
 * {@code LibraryService.addItem(...)} so the borrow-to-zero / return-activation
 * transitions are deterministic (no seeded catalogue item has exactly one copy).
 */
@ExtendWith(ApplicationExtension.class)
public class BorrowReserveReturnE2ETest extends ApplicationTest {

    private static final String STUDENT_A = "e2e.student.a@system.local";
    private static final String STUDENT_B = "e2e.student.b@system.local";
    private static final String LIBRARY_NAV = "Library / Search";
    private static final String LOANS_NAV = "Loans & Reservations";

    private String itemId;

    @Start
    public void start(Stage stage) {
        LoginView login = new LoginView();
        Navigator navigator = new Navigator(stage, login);
        login.attachNavigator(navigator);
        stage.show();
    }

    @BeforeEach
    void setUp() {
        PersonaService.reset();
        Persona.setCurrentUser(null);
        ReservationService.reset();
        addStudent(STUDENT_A);
        addStudent(STUDENT_B);
        itemId = seedSingleCopyBook();
    }

    private void addStudent(String email) {
        if (PersonaService.getProfile(email) != null) {
            return;
        }
        Persona student = new Persona(email, GuiTestSupport.MASTER_KEY);
        student.updateRole(UserRole.STUDENT);
        PersonaService.addPersona(student);
        PersonaRepository.insertPersona(student);
    }

    /** Adds a fresh single-copy physical book so borrow drops availability to 0. */
    private String seedSingleCopyBook() {
        String id = "E2E-" + System.nanoTime();
        Book book = new Book(id, "E2E Test Title " + id, "Programming", 2024);
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        book.setUnitPrice(100);
        LibraryService.addItem(book);
        return id;
    }

    /**
     * One student-A session covering every borrow/reserve/return flow end-to-end,
     * signing in through the 2FA flow once and signing out once (issue #7) rather
     * than paying the heavy GUI login for each flow:
     *
     * <ol>
     *   <li><b>Borrow</b> the seeded single copy → a loan exists and copies hit 0.</li>
     *   <li><b>Reserve while full then activate on return</b>: B queues as WAITING on
     *       that borrowed copy, A returns it through the GUI, B's reservation goes
     *       ready.</li>
     *   <li><b>Reserve when a copy is free</b>: a freshly seeded copy reserves as
     *       ready immediately.</li>
     * </ol>
     *
     * Each phase drives the real widgets and then asserts the backend state
     * actually changed (loans, available copies, reservation status).
     */
    @Test
    public void borrowReserveReturnEndToEnd() {
        GuiTestSupport.signIn(this, STUDENT_A);
        String memberA = PersonaService.getProfile(STUDENT_A).getMemberId();
        String memberB = PersonaService.getProfile(STUDENT_B).getMemberId();

        // Phase 1 — borrow the only copy: a loan is recorded and availability -> 0.
        String borrowedItem = itemId;
        clickOn(LIBRARY_NAV);
        selectSeededRow();
        clickButton("Borrow selected");
        GuiTestSupport.dismissInfo(this, "Borrowed");
        waitForServiceState(() -> hasLoan(memberA, borrowedItem));
        assertTrue(hasLoan(memberA, borrowedItem), "a loan should be recorded for the borrower");
        assertEquals(0, LibraryService.getItemById(borrowedItem).getAvailableCopies(),
                "the single copy should now be checked out");

        // Phase 2 — with 0 copies free, B's reservation queues as WAITING; A then
        // returns the copy through the GUI, which promotes B's reservation to ready.
        Reservation reservation = ReservationService.reserve(
                memberB, borrowedItem, SimulationClock.getCurrentDay());
        assertEquals(ReservationStatus.WAITING, reservation.getStatus(),
                "no copies free, so the reservation queues");

        clickOn(LOANS_NAV);
        selectFirstLoanRow();
        clickButton("Return selected");
        GuiTestSupport.dismissInfo(this, "Returned");
        waitForServiceState(() -> !hasLoan(memberA, borrowedItem));
        assertTrue(ReservationService.hasReadyReservation(memberB, borrowedItem),
                "returning the copy should activate the queued reservation");

        // Phase 3 — a freshly seeded copy is free, so reserving it is ready at once.
        itemId = seedSingleCopyBook();
        String freeItem = itemId;
        clickOn(LIBRARY_NAV);
        selectSeededRow();
        clickButton("Reserve selected");
        GuiTestSupport.dismissInfo(this, "Reserved");
        waitForServiceState(() -> !ReservationService.getMemberReservations(memberA).isEmpty());
        assertTrue(ReservationService.hasReadyReservation(memberA, freeItem),
                "a copy is available, so the reservation is ready immediately");

        GuiTestSupport.signOut(this);
    }

    /**
     * Clicks a {@link javafx.scene.control.Button} by its text. Under headless
     * Monocle, coordinate-based {@link #clickOn(String)} can miss buttons that
     * are off-screen or clipped, so we resolve the node from the scene graph and
     * fire its action handler directly — still exercising the same handler a
     * user click would.
     */
    private void clickButton(String text) {
        javafx.scene.control.Button button = lookup(text)
                .queryAs(javafx.scene.control.Button.class);
        assertNotNull(button, "button should be present: " + text);
        interact(() -> button.fire());
        WaitForAsyncUtils.waitForFxEvents();
    }

    // --- helpers -------------------------------------------------------------

    /** Selects the seeded item's row in the Library search table. */
    private void selectSeededRow() {
        WaitForAsyncUtils.waitForFxEvents();
        @SuppressWarnings("unchecked")
        TableView<LibraryItem> table =
                lookup(".table-view").queryTableView();
        assertNotNull(table, "library table should be present");
        // Results paginate at 10 rows, so filter to the unique seeded id to bring
        // it onto the first page. Set the field text directly (headless keyboard
        // focus on the search field is unreliable) — clearing any text a previous
        // phase left, since we reuse one login across several borrow/reserve
        // phases and an appended id would match nothing.
        javafx.scene.control.TextField field =
                lookup(".field").queryAs(javafx.scene.control.TextField.class);
        interact(() -> field.setText(itemId));
        // The debounced search runs on a background thread and re-renders the
        // page (which clears the table selection). Poll: keep re-selecting the
        // seeded row until the selection sticks, so a late re-render can't leave
        // us with a null selection when the Borrow/Reserve click lands.
        waitForServiceState(() -> {
            LibraryItem match = table.getItems().stream()
                    .filter(i -> i.getItemId().equals(itemId))
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                return false;
            }
            interact(() -> table.getSelectionModel().select(match));
            return match.equals(table.getSelectionModel().getSelectedItem());
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(table.getSelectionModel().getSelectedItem(),
                "seeded item should be selectable in the search results");
    }

    /** Selects the first row of the loans table on the Loans &amp; Reservations panel. */
    private void selectFirstLoanRow() {
        WaitForAsyncUtils.waitForFxEvents();
        TableView<?> loans = lookup(".table-view").nth(0).queryTableView();
        assertNotNull(loans, "loans table should be present");
        waitForServiceState(() -> !loans.getItems().isEmpty());
        interact(() -> loans.getSelectionModel().select(0));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private boolean hasLoan(String memberId, String item) {
        return LoanService.getLoans().stream()
                .anyMatch(loan -> loan.getMemberId().equals(memberId)
                        && loan.getItemId().equals(item));
    }

    private void waitForServiceState(java.util.concurrent.Callable<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, condition);
        } catch (Exception e) {
            throw new AssertionError("Timed out waiting for backend state", e);
        }
    }
}
