package ir.ac.kntu.util;

// Persistence for the {@code two_factor_codes} table. Split out of the
// former monolithic {@code DatabaseAccess} class as part of the per-domain
// repository migration.
public final class TwoFactorRepository {

    private TwoFactorRepository() {
    }

    public static void clearTwoFactorCodes() {
        Database.executeUpdate("DELETE FROM two_factor_codes");
    }

    public static void saveTwoFactorCode(String email, String code, long issuedAt) {
        Database.withPs("MERGE INTO two_factor_codes USING (VALUES (?, ?, ?)) AS s(email, code, issued_at) ON two_factor_codes.email = s.email WHEN MATCHED THEN UPDATE SET code = s.code, issued_at = s.issued_at WHEN NOT MATCHED THEN INSERT (email, code, issued_at) VALUES (s.email, s.code, s.issued_at)", ps -> {
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
}
