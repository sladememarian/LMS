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

    @Test
    public void borrowFromLibraryCreatesLoanAndDecrementsCopies() {
        GuiTestSupport.signIn(this, STUDENT_A);
        String memberId = PersonaService.getProfile(STUDENT_A).getMemberId();

        clickOn(LIBRARY_NAV);
        selectSeededRow();
        clickOn("Borrow selected");
        GuiTestSupport.dismissInfo(this, "Borrowed");

        // Backend state actually changed: a loan exists and the copy is gone.
        waitForServiceState(() -> hasLoan(memberId, itemId));
        assertTrue(hasLoan(memberId, itemId), "a loan should be recorded for the borrower");
        assertEquals(0, LibraryService.getItemById(itemId).getAvailableCopies(),
                "the single copy should now be checked out");
    }

    @Test
    public void reserveWaitsWhenNoCopiesAndActivatesOnReturn() {
        // Student A borrows the only copy so B's reservation must queue as WAITING.
        GuiTestSupport.signIn(this, STUDENT_A);
        String memberA = PersonaService.getProfile(STUDENT_A).getMemberId();
        String memberB = PersonaService.getProfile(STUDENT_B).getMemberId();

        clickOn(LIBRARY_NAV);
        selectSeededRow();
        clickOn("Borrow selected");
        GuiTestSupport.dismissInfo(this, "Borrowed");
        waitForServiceState(() -> hasLoan(memberA, itemId));

        // B reserves through the service the same way the GUI does; with 0 copies
        // free it must be WAITING. (Second GUI login is heavy; the reserve button
        // path itself is covered by the borrow test's navigation.)
        Reservation reservation = ReservationService.reserve(
                memberB, itemId, SimulationClock.getCurrentDay());
        assertEquals(ReservationStatus.WAITING, reservation.getStatus(),
                "no copies free, so the reservation queues");

        // A returns the copy through the GUI; the return promotes B's reservation.
        clickOn(LOANS_NAV);
        selectFirstLoanRow();
        clickOn("Return selected");
        GuiTestSupport.dismissInfo(this, "Returned");

        waitForServiceState(() -> !hasLoan(memberA, itemId));
        assertTrue(ReservationService.hasReadyReservation(memberB, itemId),
                "returning the copy should activate the queued reservation");
    }

    @Test
    public void reserveWhenCopyAvailableIsImmediatelyActive() {
        GuiTestSupport.signIn(this, STUDENT_A);
        String memberId = PersonaService.getProfile(STUDENT_A).getMemberId();

        clickOn(LIBRARY_NAV);
        selectSeededRow();
        clickOn("Reserve selected");
        GuiTestSupport.dismissInfo(this, "Reserved");

        waitForServiceState(() -> !ReservationService.getMemberReservations(memberId).isEmpty());
        assertTrue(ReservationService.hasReadyReservation(memberId, itemId),
                "a copy is available, so the reservation is ready immediately");
    }

    // --- helpers -------------------------------------------------------------

    /** Selects the seeded item's row in the Library search table. */
    private void selectSeededRow() {
        WaitForAsyncUtils.waitForFxEvents();
        @SuppressWarnings("unchecked")
        TableView<LibraryItem> table =
                lookup(".table-view").queryTableView();
        assertNotNull(table, "library table should be present");
        // Results paginate at 10 rows, so filter to the unique seeded title
        // (which embeds the item id) to bring it onto the first page.
        clickOn(".field").write(itemId);
        // The debounced search then runs on a background thread; wait until the
        // seeded item shows up in the (now filtered) table before selecting it.
        waitForServiceState(() -> table.getItems().stream()
                .anyMatch(i -> i.getItemId().equals(itemId)));
        interact(() -> {
            for (LibraryItem item : table.getItems()) {
                if (item.getItemId().equals(itemId)) {
                    table.getSelectionModel().select(item);
                    break;
                }
            }
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
