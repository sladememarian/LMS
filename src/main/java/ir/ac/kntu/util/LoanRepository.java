package ir.ac.kntu.util;

import java.util.List;

import ir.ac.kntu.finance.Loan;

// Persistence for the {@code loans} table (composite key: member_id +
// item_id). Split out of the former monolithic {@code DatabaseAccess} class
// as part of the per-domain repository migration.
public final class LoanRepository {

    private LoanRepository() {
    }

    public static void clearLoans() {
        Database.executeUpdate("DELETE FROM loans");
    }

    public static void insertLoan(Loan loan) {
        Database.withPs("MERGE INTO loans USING (VALUES (?, ?, ?, ?, ?)) AS s(member_id, item_id, borrow_day, due_day, last_charged_day) ON loans.member_id = s.member_id AND loans.item_id = s.item_id WHEN MATCHED THEN UPDATE SET borrow_day = s.borrow_day, due_day = s.due_day, last_charged_day = s.last_charged_day WHEN NOT MATCHED THEN INSERT (member_id, item_id, borrow_day, due_day, last_charged_day) VALUES (s.member_id, s.item_id, s.borrow_day, s.due_day, s.last_charged_day)", ps -> {
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
}
