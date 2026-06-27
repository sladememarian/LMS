package ir.ac.kntu.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class DatabaseAccess {

    public static void clearPersonas() {
        Database.executeUpdate("DELETE FROM borrowed_items");
        Database.executeUpdate("DELETE FROM personas");
    }

    public static void insertPersona(Persona persona) {
        String email = resolveEmail(persona);
        Database.withPs("INSERT INTO personas (email, username, password, role, member_id, wallet_balance, first_name, last_name, phone, theme) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (email) DO UPDATE SET username = EXCLUDED.username, password = EXCLUDED.password, role = EXCLUDED.role, member_id = EXCLUDED.member_id, wallet_balance = EXCLUDED.wallet_balance, first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, phone = EXCLUDED.phone, theme = EXCLUDED.theme", ps -> {
            ps.setString(1, email);
            ps.setString(2, persona.getUsername());
            ps.setString(3, persona.getPassword());
            ps.setString(4, persona.getRole().name());
            ps.setString(5, persona.getMemberId());
            ps.setInt(6, persona.getWalletBalance());
            ps.setString(7, persona.getFirstName());
            ps.setString(8, persona.getLastName());
            ps.setString(9, persona.getPhoneNumber());
            ps.setString(10, persona.getTheme());
            ps.executeUpdate();
        });
    }

    private static String resolveEmail(Persona persona) {
        if (persona.getEmail() != null) {
            return persona.getEmail();
        }
        return persona.getUsername() + "@system.local";
    }

    private static boolean isSystemEmail(String email) {
        return email != null && email.endsWith("@system.local");
    }

    public static List<Persona> getAllPersonas() {
        return Database.queryAll("SELECT * FROM personas", DatabaseAccess::mapPersona);
    }

    private static Persona mapPersona(ResultSet rs) throws SQLException {
        String email = rs.getString("email");
        String username = rs.getString("username");
        String password = rs.getString("password");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        String memberId = rs.getString("member_id");
        int wallet = rs.getInt("wallet_balance");
        Persona persona = isSystemEmail(email)
            ? new Persona(null, username, password, role, memberId, wallet)
            : new Persona(email, username, password, role, memberId, wallet);
        persona.setFirstName(rs.getString("first_name"));
        persona.setLastName(rs.getString("last_name"));
        persona.setPhoneNumber(rs.getString("phone"));
        persona.setTheme(rs.getString("theme"));
        if (!isSystemEmail(email)) {
            Database.withPs("SELECT item_id FROM borrowed_items WHERE email=?", ps -> {
                ps.setString(1, email);
                try (ResultSet brs = ps.executeQuery()) {
                    while (brs.next()) {
                        persona.addBorrowedItem(brs.getString("item_id"));
                    }
                }
            });
        }
        return persona;
    }

    public static void saveBorrowedItems(Persona persona) {
        String email = resolveEmail(persona);
        Database.withPs("DELETE FROM borrowed_items WHERE email=?", ps -> {
            ps.setString(1, email);
            ps.executeUpdate();
        });
        Database.withPs("INSERT INTO borrowed_items (email, item_id) VALUES (?, ?)", ps -> {
            for (String itemId : persona.getBorrowedItemIds()) {
                ps.setString(1, email);
                ps.setString(2, itemId);
                ps.executeUpdate();
            }
        });
    }

    public static void clearMailMessages() {
        Database.executeUpdate("DELETE FROM mail_messages");
    }

    public static void insertMailMessage(MailMessage msg) {
        Database.withPs("INSERT INTO mail_messages (message_id, recipient_email, subject, body, type, sent_date, is_read) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, msg.getMessageId());
            ps.setString(2, msg.getRecipientEmail());
            ps.setString(3, msg.getSubject());
            ps.setString(4, msg.getBody());
            ps.setString(5, msg.getMessageType().getLabel());
            ps.setString(6, msg.getSentDate());
            ps.setBoolean(7, msg.isRead());
            ps.executeUpdate();
        });
    }

    public static List<MailMessage> getAllMailMessages() {
        return Database.queryAll("SELECT * FROM mail_messages", rs -> {
            MailMessage msg = new MailMessage(rs.getString("recipient_email"), rs.getString("subject"),
                    rs.getString("body"), MessageType.fromLabel(rs.getString("type")));
            msg.setMessageId(rs.getString("message_id"));
            msg.setSentDate(rs.getString("sent_date"));
            msg.setRead(rs.getBoolean("is_read"));
            return msg;
        });
    }

    public static void deleteMailMessagesForRecipient(String recipient) {
        Database.withPs("DELETE FROM mail_messages WHERE recipient_email=?", ps -> {
            ps.setString(1, recipient);
            ps.executeUpdate();
        });
    }

    public static void markMailRead(String recipient) {
        Database.withPs("UPDATE mail_messages SET is_read=TRUE WHERE recipient_email=?", ps -> {
            ps.setString(1, recipient);
            ps.executeUpdate();
        });
    }

    public static void clearTwoFactorCodes() {
        Database.executeUpdate("DELETE FROM two_factor_codes");
    }

    public static void saveTwoFactorCode(String email, String code, long issuedAt) {
        Database.withPs("INSERT INTO two_factor_codes (email, code, issued_at) VALUES (?, ?, ?) ON CONFLICT (email) DO UPDATE SET code = EXCLUDED.code, issued_at = EXCLUDED.issued_at", ps -> {
            ps.setString(1, email.toLowerCase());
            ps.setString(2, code);
            ps.setLong(3, issuedAt);
            ps.executeUpdate();
        });
    }

    public static String getTwoFactorCode(String email) {
        return Database.queryPrepared("SELECT code FROM two_factor_codes WHERE email=?",
                ps -> ps.setString(1, email.toLowerCase()), rs -> rs.getString("code"));
    }

    public static long getTwoFactorCodeIssuedAt(String email) {
        Long value = Database.queryPrepared("SELECT issued_at FROM two_factor_codes WHERE email=?",
                ps -> ps.setString(1, email.toLowerCase()), rs -> rs.getLong("issued_at"));
        return value != null ? value : 0L;
    }

    public static void removeTwoFactorCode(String email) {
        Database.withPs("DELETE FROM two_factor_codes WHERE email=?", ps -> {
            ps.setString(1, email.toLowerCase());
            ps.executeUpdate();
        });
    }

    public static void clearTransactions() {
        Database.executeUpdate("DELETE FROM transactions");
    }

    public static void insertTransaction(Transaction tx) {
        Database.withPs("INSERT INTO transactions (tx_id, member_id, amount, type, description, timestamp) VALUES (?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getMemberId());
            ps.setInt(3, tx.getAmount());
            ps.setString(4, tx.getType());
            ps.setString(5, tx.getDescription());
            ps.setLong(6, tx.getTimestamp());
            ps.executeUpdate();
        });
    }

    public static List<Transaction> getAllTransactions() {
        return Database.queryAll("SELECT * FROM transactions", rs -> new Transaction(
                rs.getString("tx_id"), rs.getString("member_id"), rs.getInt("amount"),
                rs.getString("type"), rs.getString("description"), rs.getLong("timestamp")));
    }

    public static void clearLoans() {
        Database.executeUpdate("DELETE FROM loans");
    }

    public static void insertLoan(Loan loan) {
        Database.withPs("INSERT INTO loans (member_id, item_id, borrow_day, due_day, last_charged_day) VALUES (?, ?, ?, ?, ?) ON CONFLICT (member_id, item_id) DO UPDATE SET borrow_day = EXCLUDED.borrow_day, due_day = EXCLUDED.due_day, last_charged_day = EXCLUDED.last_charged_day", ps -> {
            ps.setString(1, loan.getMemberId());
            ps.setString(2, loan.getItemId());
            ps.setInt(3, loan.getBorrowDay());
            ps.setInt(4, loan.getDueDay());
            ps.setInt(5, loan.getLastChargedDay());
            ps.executeUpdate();
        });
    }

    public static List<Loan> getAllLoans() {
        return Database.queryAll("SELECT * FROM loans", rs -> {
            Loan loan = new Loan(rs.getString("member_id"), rs.getString("item_id"),
                    rs.getInt("borrow_day"), rs.getInt("due_day"));
            loan.setLastChargedDay(rs.getInt("last_charged_day"));
            return loan;
        });
    }

    public static void deleteLoan(String memberId, String itemId) {
        Database.withPs("DELETE FROM loans WHERE member_id=? AND item_id=?", ps -> {
            ps.setString(1, memberId);
            ps.setString(2, itemId);
            ps.executeUpdate();
        });
    }

    public static void clearClock() {
        Database.executeUpdate("DELETE FROM clock_state");
    }

    public static void saveClock(int currentDay, LocalDate startDate) {
        Database.withPs("INSERT INTO clock_state (id, current_day, start_date) VALUES (1, ?, ?) ON CONFLICT (id) DO UPDATE SET current_day = EXCLUDED.current_day, start_date = EXCLUDED.start_date", ps -> {
            ps.setInt(1, currentDay);
            ps.setString(2, startDate.toString());
            ps.executeUpdate();
        });
    }

    public static Map<String, Object> loadClock() {
        return Database.querySingle("SELECT current_day, start_date FROM clock_state WHERE id=1", rs -> {
            Map<String, Object> result = new HashMap<>();
            result.put("currentDay", rs.getInt("current_day"));
            result.put("startDate", LocalDate.parse(rs.getString("start_date")));
            return result;
        });
    }

    public static void clearSuppliers() {
        Database.executeUpdate("DELETE FROM suppliers");
    }

    public static void insertSupplier(SupplierCompany supplier) {
        Database.withPs("INSERT INTO suppliers (company_id, company_name) VALUES (?, ?) ON CONFLICT (company_id) DO UPDATE SET company_name = EXCLUDED.company_name", ps -> {
            ps.setString(1, supplier.getCompanyId());
            ps.setString(2, supplier.getCompanyName());
            ps.executeUpdate();
        });
    }

    public static List<SupplierCompany> getAllSuppliers() {
        return Database.queryAll("SELECT * FROM suppliers",
                rs -> new SupplierCompany(rs.getString("company_id"), rs.getString("company_name")));
    }

    public static void clearLibraryItems() {
        Database.executeUpdate("DELETE FROM library_items");
    }

    public static void insertLibraryItem(LibraryItem item) {
        Database.withPs("INSERT INTO library_items (item_id, type, title, category, publish_year, supplier_id, total_copies, available_copies, unit_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (item_id) DO UPDATE SET type = EXCLUDED.type, title = EXCLUDED.title, category = EXCLUDED.category, publish_year = EXCLUDED.publish_year, supplier_id = EXCLUDED.supplier_id, total_copies = EXCLUDED.total_copies, available_copies = EXCLUDED.available_copies, unit_price = EXCLUDED.unit_price", ps -> {
            ps.setString(1, item.getItemId());
            ps.setString(2, item.getItemType());
            ps.setString(3, item.getTitle());
            ps.setString(4, item.getCategory());
            ps.setInt(5, item.getPublishYear());
            ps.setString(6, item.getSupplierId());
            ps.setInt(7, item.getTotalCopies());
            ps.setInt(8, item.getAvailableCopies());
            ps.setInt(9, item.getUnitPrice());
            ps.executeUpdate();
        });
    }

    public static void deleteLibraryItem(String itemId) {
        Database.withPs("DELETE FROM library_items WHERE item_id=?", ps -> {
            ps.setString(1, itemId);
            ps.executeUpdate();
        });
    }

    public static List<LibraryItem> getAllLibraryItems() {
        return Database.queryAll("SELECT * FROM library_items", DatabaseAccess::mapLibraryItem);
    }

    private static LibraryItem mapLibraryItem(ResultSet rs) throws SQLException {
        String id = rs.getString("item_id");
        String title = rs.getString("title");
        String category = rs.getString("category");
        int year = rs.getInt("publish_year");
        String type = rs.getString("type");
        LibraryItem item;
        switch (type) {
            case "BOOK":
                item = new Book(id, title, category, year);
                break;
            case "EBOOK":
                item = new EBook(id, title, category, year);
                break;
            case "MAGAZINE":
                item = new Magazine(id, title, category, year);
                break;
            case "AUDIOBOOK":
                item = new AudioBook(id, title, category, year);
                break;
            default:
                item = new Book(id, title, category, year);
                break;
        }
        item.setSupplierId(rs.getString("supplier_id"));
        item.setTotalCopies(rs.getInt("total_copies"));
        item.setAvailableCopies(rs.getInt("available_copies"));
        item.setUnitPrice(rs.getInt("unit_price"));
        return item;
    }

    public static void clearSupportTickets() {
        Database.executeUpdate("DELETE FROM support_tickets");
    }

    public static void insertSupportTicket(SupportTicket ticket) {
        Database.withPs("INSERT INTO support_tickets (ticket_id, user_id, title, description, category, priority, status, response) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (ticket_id) DO UPDATE SET user_id = EXCLUDED.user_id, title = EXCLUDED.title, description = EXCLUDED.description, category = EXCLUDED.category, priority = EXCLUDED.priority, status = EXCLUDED.status, response = EXCLUDED.response", ps -> {
            ps.setString(1, ticket.getTicketId());
            ps.setString(2, ticket.getUserId());
            ps.setString(3, ticket.getTitle());
            ps.setString(4, ticket.getDescription());
            ps.setString(5, ticket.getCategory());
            ps.setString(6, ticket.getPriority());
            ps.setString(7, ticket.getStatus());
            ps.setString(8, ticket.getResponse());
            ps.executeUpdate();
        });
    }

    public static List<SupportTicket> getAllSupportTickets() {
        return Database.queryAll("SELECT * FROM support_tickets", rs -> {
            SupportTicket ticket = new SupportTicket(rs.getString("ticket_id"), rs.getString("user_id"),
                    rs.getString("title"), rs.getString("description"));
            ticket.setCategory(rs.getString("category"));
            ticket.setPriority(rs.getString("priority"));
            ticket.setStatus(rs.getString("status"));
            ticket.setResponse(rs.getString("response"));
            return ticket;
        });
    }

    public static void updateSupportTicketStatus(String ticketId, String status) {
        Database.withPs("UPDATE support_tickets SET status=? WHERE ticket_id=?", ps -> {
            ps.setString(1, status);
            ps.setString(2, ticketId);
            ps.executeUpdate();
        });
    }

    public static void updateSupportTicketResponse(String ticketId, String response, String status) {
        Database.withPs("UPDATE support_tickets SET response=?, status=? WHERE ticket_id=?", ps -> {
            ps.setString(1, response);
            ps.setString(2, status);
            ps.setString(3, ticketId);
            ps.executeUpdate();
        });
    }

    public static void clearRoleRequests() {
        Database.executeUpdate("DELETE FROM role_requests");
    }

    public static void insertRoleRequest(RoleRequest request) {
        Database.withPs("INSERT INTO role_requests (request_id, requester_email, requested_role, message, status) VALUES (?, ?, ?, ?, ?) ON CONFLICT (request_id) DO UPDATE SET requester_email = EXCLUDED.requester_email, requested_role = EXCLUDED.requested_role, message = EXCLUDED.message, status = EXCLUDED.status", ps -> {
            ps.setString(1, request.getRequestId());
            ps.setString(2, request.getRequesterEmail());
            ps.setString(3, request.getRequestedRole());
            ps.setString(4, request.getMessage());
            ps.setString(5, request.getStatus());
            ps.executeUpdate();
        });
    }

    public static List<RoleRequest> getAllRoleRequests() {
        return Database.queryAll("SELECT * FROM role_requests", rs -> {
            RoleRequest request = new RoleRequest(rs.getString("request_id"), rs.getString("requester_email"),
                    rs.getString("requested_role"), rs.getString("message"));
            request.setStatus(rs.getString("status"));
            return request;
        });
    }

    public static void updateRoleRequestStatus(String requestId, String status) {
        Database.withPs("UPDATE role_requests SET status=? WHERE request_id=?", ps -> {
            ps.setString(1, status);
            ps.setString(2, requestId);
            ps.executeUpdate();
        });
    }
}
