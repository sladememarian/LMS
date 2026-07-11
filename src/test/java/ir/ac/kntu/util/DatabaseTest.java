package ir.ac.kntu.util;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ir.ac.kntu.finance.Loan;
import ir.ac.kntu.finance.Transaction;
import ir.ac.kntu.library.AudioBook;
import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.EBook;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.Magazine;
import ir.ac.kntu.library.SupplierCompany;
import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.rolerequest.RoleRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @BeforeAll
    static void setup() {
        Database.getConnection();
    }

    @BeforeEach
    void clean() {
        DatabaseAccess.clearPersonas();
        DatabaseAccess.clearMailMessages();
        DatabaseAccess.clearTwoFactorCodes();
        DatabaseAccess.clearTransactions();
        DatabaseAccess.clearLoans();
        DatabaseAccess.clearClock();
        DatabaseAccess.clearSuppliers();
        DatabaseAccess.clearLibraryItems();
        DatabaseAccess.clearSupportTickets();
        DatabaseAccess.clearRoleRequests();
    }

    @Test
    void insertAndRetrievePersona() {
        Persona p = new Persona("test@example.com", "password123");
        DatabaseAccess.insertPersona(p);

        List<Persona> all = DatabaseAccess.getAllPersonas();
        assertEquals(1, all.size());
        assertEquals("test@example.com", all.get(0).getEmail());
        assertEquals(UserRole.GUEST, all.get(0).getRole());
    }

    @Test
    void insertAndRetrieveSystemAccount() {
        Persona p = new Persona("admin", "adminpass", UserRole.ADMIN);
        DatabaseAccess.insertPersona(p);

        List<Persona> all = DatabaseAccess.getAllPersonas();
        assertEquals(1, all.size());
        assertEquals("admin", all.get(0).getUsername());
        assertEquals(UserRole.ADMIN, all.get(0).getRole());
    }

    @Test
    void updatePersona() {
        Persona p = new Persona("update@test.com", "pass123");
        DatabaseAccess.insertPersona(p);

        p.setWalletBalance(1000);
        p.setTheme("DARK");
        DatabaseAccess.insertPersona(p);

        List<Persona> all = DatabaseAccess.getAllPersonas();
        assertEquals(1, all.size());
        assertEquals(1000, all.get(0).getWalletBalance());
        assertEquals("DARK", all.get(0).getTheme());
    }

    @Test
    void saveAndLoadBorrowedItems() {
        Persona p = new Persona("borrow@test.com", "pass123");
        p.addBorrowedItem("ITEM-001");
        p.addBorrowedItem("ITEM-002");
        DatabaseAccess.insertPersona(p);
        DatabaseAccess.saveBorrowedItems(p);

        List<Persona> all = DatabaseAccess.getAllPersonas();
        assertEquals(1, all.size());
        List<String> items = all.get(0).getBorrowedItemIds();
        assertTrue(items.contains("ITEM-001"));
        assertTrue(items.contains("ITEM-002"));
    }

    @Test
    void insertAndRetrieveMailMessage() {
        MailMessage msg = new MailMessage("user@test.com", "Subject", "Body text", MessageType.WELCOME);
        DatabaseAccess.insertMailMessage(msg);

        List<MailMessage> all = DatabaseAccess.getAllMailMessages();
        assertEquals(1, all.size());
        assertEquals("user@test.com", all.get(0).getRecipientEmail());
        assertEquals("Subject", all.get(0).getSubject());
        assertEquals(MessageType.WELCOME, all.get(0).getMessageType());
    }

    @Test
    void markMailRead() {
        MailMessage msg = new MailMessage("read@test.com", "Sub", "Body", MessageType.SYSTEM_NOTIFICATION);
        DatabaseAccess.insertMailMessage(msg);
        DatabaseAccess.markMailRead("read@test.com");

        List<MailMessage> all = DatabaseAccess.getAllMailMessages();
        assertTrue(all.get(0).isRead());
    }

    @Test
    void deleteMailMessages() {
        MailMessage msg = new MailMessage("del@test.com", "Sub", "Body", MessageType.TWO_FA);
        DatabaseAccess.insertMailMessage(msg);
        DatabaseAccess.deleteMailMessagesForRecipient("del@test.com");

        assertEquals(0, DatabaseAccess.getAllMailMessages().size());
    }

    @Test
    void twoFactorCodeLifecycle() {
        DatabaseAccess.saveTwoFactorCode("2fa@test.com", "123456", 1000L);
        assertEquals("123456", DatabaseAccess.getTwoFactorCode("2fa@test.com"));
        assertEquals(1000L, DatabaseAccess.getTwoFactorCodeIssuedAt("2fa@test.com"));

        DatabaseAccess.removeTwoFactorCode("2fa@test.com");
        assertNull(DatabaseAccess.getTwoFactorCode("2fa@test.com"));
    }

    @Test
    void insertAndRetrieveTransactions() {
        Transaction tx = new Transaction("TX-001", "MEM-001", 500, "CHARGE", "Wallet topup", 1000L);
        DatabaseAccess.insertTransaction(tx);

        List<Transaction> all = DatabaseAccess.getAllTransactions();
        assertEquals(1, all.size());
        assertEquals("TX-001", all.get(0).getTransactionId());
        assertEquals(500, all.get(0).getAmount());
        assertEquals("CHARGE", all.get(0).getType());
    }

    @Test
    void insertAndRetrieveLoan() {
        Loan loan = new Loan("MEM-001", "ITEM-001", 1, 4);
        DatabaseAccess.insertLoan(loan);

        List<Loan> all = DatabaseAccess.getAllLoans();
        assertEquals(1, all.size());
        assertEquals("MEM-001", all.get(0).getMemberId());
        assertEquals("ITEM-001", all.get(0).getItemId());
        assertEquals(4, all.get(0).getDueDay());
    }

    @Test
    void deleteLoan() {
        DatabaseAccess.insertLoan(new Loan("MEM-001", "ITEM-001", 1, 4));
        DatabaseAccess.deleteLoan("MEM-001", "ITEM-001");
        assertEquals(0, DatabaseAccess.getAllLoans().size());
    }

    @Test
    void clockSaveAndLoad() {
        DatabaseAccess.saveClock(10, LocalDate.of(2024, 1, 15));

        Map<String, Object> data = DatabaseAccess.loadClock();
        assertNotNull(data);
        assertEquals(10, data.get("currentDay"));
        assertEquals(LocalDate.of(2024, 1, 15), data.get("startDate"));
    }

    @Test
    void insertAndRetrieveSupplier() {
        DatabaseAccess.insertSupplier(new SupplierCompany("SUP-001", "Test Supplier"));

        List<SupplierCompany> all = DatabaseAccess.getAllSuppliers();
        assertEquals(1, all.size());
        assertEquals("SUP-001", all.get(0).getCompanyId());
        assertEquals("Test Supplier", all.get(0).getCompanyName());
    }

    @Test
    void insertAndRetrieveLibraryItems() {
        Book book = new Book("ITEM-B001", "Test Book", "Fiction", 2023);
        book.setSupplierId("SUP-001");
        book.setTotalCopies(5);
        book.setAvailableCopies(3);
        book.setUnitPrice(100);
        DatabaseAccess.insertLibraryItem(book);

        EBook ebook = new EBook("ITEM-E001", "Test EBook", "Tech", 2024);
        ebook.setSupplierId("SUP-002");
        ebook.setTotalCopies(999);
        ebook.setAvailableCopies(999);
        ebook.setUnitPrice(50);
        DatabaseAccess.insertLibraryItem(ebook);

        Magazine mag = new Magazine("ITEM-M001", "Test Mag", "Science", 2024);
        mag.setSupplierId("SUP-003");
        mag.setTotalCopies(10);
        mag.setAvailableCopies(10);
        mag.setUnitPrice(30);
        DatabaseAccess.insertLibraryItem(mag);

        AudioBook ab = new AudioBook("ITEM-A001", "Test Audio", "History", 2022);
        ab.setSupplierId("SUP-002");
        ab.setTotalCopies(5);
        ab.setAvailableCopies(4);
        ab.setUnitPrice(80);
        DatabaseAccess.insertLibraryItem(ab);

        List<LibraryItem> all = DatabaseAccess.getAllLibraryItems();
        assertEquals(4, all.size());

        LibraryItem loadedBook = all.stream().filter(i -> i.getItemId().equals("ITEM-B001")).findFirst().orElse(null);
        assertNotNull(loadedBook);
        assertEquals("BOOK", loadedBook.getItemType());
        assertEquals(3, loadedBook.getAvailableCopies());

        LibraryItem loadedEbook = all.stream().filter(i -> i.getItemId().equals("ITEM-E001")).findFirst().orElse(null);
        assertNotNull(loadedEbook);
        assertEquals("EBOOK", loadedEbook.getItemType());
    }

    @Test
    void deleteLibraryItem() {
        Book book = new Book("ITEM-DEL", "Delete Me", "Fiction", 2023);
        book.setSupplierId("SUP-001");
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        book.setUnitPrice(10);
        DatabaseAccess.insertLibraryItem(book);
        assertEquals(1, DatabaseAccess.getAllLibraryItems().size());

        DatabaseAccess.deleteLibraryItem("ITEM-DEL");
        assertEquals(0, DatabaseAccess.getAllLibraryItems().size());
    }

    @Test
    void insertAndRetrieveSupportTicket() {
        SupportTicket ticket = new SupportTicket("TCK-001", "USER-001", "Help!", "I need assistance", ir.ac.kntu.support.SupportSection.TECHNICAL);
        ticket.setPriority("HIGH");
        ticket.setStatus("OPEN");
        DatabaseAccess.insertSupportTicket(ticket);

        List<SupportTicket> all = DatabaseAccess.getAllSupportTickets();
        assertEquals(1, all.size());
        assertEquals("TCK-001", all.get(0).getTicketId());
        assertEquals("OPEN", all.get(0).getStatus());
    }

    @Test
    void updateSupportTicket() {
        SupportTicket ticket = new SupportTicket("TCK-002", "USER-002", "Bug", "Something broke", ir.ac.kntu.support.SupportSection.TECHNICAL);
        DatabaseAccess.insertSupportTicket(ticket);

        DatabaseAccess.updateSupportTicketStatus("TCK-002", "CLOSED");
        List<SupportTicket> all = DatabaseAccess.getAllSupportTickets();
        assertEquals("CLOSED", all.get(0).getStatus());

        DatabaseAccess.updateSupportTicketResponse("TCK-002", "Fixed!", "RESOLVED");
        all = DatabaseAccess.getAllSupportTickets();
        assertEquals("RESOLVED", all.get(0).getStatus());
        assertEquals("Fixed!", all.get(0).getResponse());
    }

    @Test
    void insertAndRetrieveRoleRequest() {
        RoleRequest rr = new RoleRequest("RR-001", "user@test.com", "STUDENT", "Please upgrade me");
        DatabaseAccess.insertRoleRequest(rr);

        List<RoleRequest> all = DatabaseAccess.getAllRoleRequests();
        assertEquals(1, all.size());
        assertEquals("RR-001", all.get(0).getRequestId());
        assertEquals("STUDENT", all.get(0).getRequestedRole());
        assertEquals("PENDING", all.get(0).getStatus());
    }

    @Test
    void updateRoleRequestStatus() {
        RoleRequest rr = new RoleRequest("RR-002", "user@test.com", "TEACHER", "Upgrade");
        DatabaseAccess.insertRoleRequest(rr);

        DatabaseAccess.updateRoleRequestStatus("RR-002", "APPROVED");
        List<RoleRequest> all = DatabaseAccess.getAllRoleRequests();
        assertEquals("APPROVED", all.get(0).getStatus());
    }

    @Test
    void clearAllTables() {
        DatabaseAccess.insertPersona(new Persona("clear@test.com", "pass"));
        DatabaseAccess.insertMailMessage(new MailMessage("clear@test.com", "S", "B", MessageType.WELCOME));
        DatabaseAccess.insertTransaction(new Transaction("TX-CLR", "M1", 100, "CHARGE", "test", 1L));
        DatabaseAccess.insertLoan(new Loan("M1", "I1", 1, 4));
        DatabaseAccess.insertSupplier(new SupplierCompany("S1", "S"));
        DatabaseAccess.insertLibraryItem(new Book("I1", "B", "C", 2020));
        DatabaseAccess.insertSupportTicket(new SupportTicket("T1", "U1", "T", "D", ir.ac.kntu.support.SupportSection.TECHNICAL));
        DatabaseAccess.insertRoleRequest(new RoleRequest("R1", "e@t.com", "STUDENT", "m"));

        DatabaseAccess.clearPersonas();
        DatabaseAccess.clearMailMessages();
        DatabaseAccess.clearTransactions();
        DatabaseAccess.clearLoans();
        DatabaseAccess.clearSuppliers();
        DatabaseAccess.clearLibraryItems();
        DatabaseAccess.clearSupportTickets();
        DatabaseAccess.clearRoleRequests();

        assertEquals(0, DatabaseAccess.getAllPersonas().size());
        assertEquals(0, DatabaseAccess.getAllMailMessages().size());
        assertEquals(0, DatabaseAccess.getAllTransactions().size());
        assertEquals(0, DatabaseAccess.getAllLoans().size());
        assertEquals(0, DatabaseAccess.getAllSuppliers().size());
        assertEquals(0, DatabaseAccess.getAllLibraryItems().size());
        assertEquals(0, DatabaseAccess.getAllSupportTickets().size());
        assertEquals(0, DatabaseAccess.getAllRoleRequests().size());
    }

    @Test
    void connectionManagement() {
        assertNotNull(Database.getConnection());
        Database.closeConnection();
        assertNotNull(Database.getConnection());
    }
}
