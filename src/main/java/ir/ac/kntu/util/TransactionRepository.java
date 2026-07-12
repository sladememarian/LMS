package ir.ac.kntu.util;

import java.util.List;

import ir.ac.kntu.finance.Transaction;

/**
 * Persistence for the append-only {@code transactions} table. Split out of
 * the former monolithic {@code DatabaseAccess} class as part of the
 * per-domain repository migration.
 */
public final class TransactionRepository {

    private TransactionRepository() {
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
}
