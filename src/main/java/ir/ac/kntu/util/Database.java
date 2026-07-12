package ir.ac.kntu.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private static final String ERR_PREFIX = "Database error: ";

    private static Connection connection;

    public static synchronized Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        String url = resolveDbConfig("JDBC_URL", null);
        if (url == null) {
            throw new DatabaseException(
                "JDBC_URL is not configured. Set it as an environment variable or in .env",
                null
            );
        }
        String user = resolveDbConfig("JDBC_USER", "sa");
        String password = resolveDbConfig("JDBC_PASSWORD", "");
        try {
            connection = DriverManager.getConnection(url, user, password);
            initTables();
        } catch (SQLException e) {
            throw new DatabaseException(
                "Failed to connect to database: " + e.getMessage(),
                e
            );
        }
        return connection;
    }

    private static String resolveDbConfig(String key, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return EnvConfig.get(key, defaultValue);
    }

    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
            connection = null;
        }
    }

    private static void initTables() {
        for (String sql : TABLES_SQL) {
            executeUpdate(sql);
        }
    }

    private static final String[] TABLES_SQL = {
        "CREATE TABLE IF NOT EXISTS personas (email VARCHAR(255) PRIMARY KEY, username VARCHAR(255), password VARCHAR(255) NOT NULL, role VARCHAR(50) NOT NULL DEFAULT 'GUEST', member_id VARCHAR(50), wallet_balance INTEGER DEFAULT 0, first_name VARCHAR(255), last_name VARCHAR(255), phone VARCHAR(50), theme VARCHAR(50) DEFAULT 'LIGHT', created_by VARCHAR(255))",
        "CREATE TABLE IF NOT EXISTS borrowed_items (email VARCHAR(255) NOT NULL, item_id VARCHAR(50) NOT NULL, PRIMARY KEY (email, item_id))",
        "CREATE TABLE IF NOT EXISTS mail_messages (message_id VARCHAR(50) PRIMARY KEY, recipient_email VARCHAR(255) NOT NULL, subject VARCHAR(500), body TEXT, type VARCHAR(50), sent_date VARCHAR(100), is_read BOOLEAN DEFAULT FALSE)",
        "CREATE TABLE IF NOT EXISTS two_factor_codes (email VARCHAR(255) PRIMARY KEY, code VARCHAR(10) NOT NULL, issued_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS transactions (tx_id VARCHAR(50) PRIMARY KEY, member_id VARCHAR(50), amount INTEGER NOT NULL, type VARCHAR(50), description VARCHAR(500), timestamp BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS loans (member_id VARCHAR(50) NOT NULL, item_id VARCHAR(50) NOT NULL, borrow_day INTEGER NOT NULL, due_day INTEGER NOT NULL, last_charged_day INTEGER NOT NULL, PRIMARY KEY (member_id, item_id))",
        "CREATE TABLE IF NOT EXISTS clock_state (id INTEGER DEFAULT 1 PRIMARY KEY, current_day INTEGER NOT NULL DEFAULT 1, start_date VARCHAR(20) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS suppliers (company_id VARCHAR(50) PRIMARY KEY, company_name VARCHAR(255) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS library_items (item_id VARCHAR(50) PRIMARY KEY, type VARCHAR(50) NOT NULL, title VARCHAR(500) NOT NULL, category VARCHAR(255), publish_year INTEGER, supplier_id VARCHAR(50), total_copies INTEGER DEFAULT 0, available_copies INTEGER DEFAULT 0, unit_price INTEGER DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS support_tickets (ticket_id VARCHAR(50) PRIMARY KEY, user_id VARCHAR(50), title VARCHAR(500), description TEXT, section VARCHAR(100), priority VARCHAR(50), status VARCHAR(50), response TEXT)",
        "CREATE TABLE IF NOT EXISTS role_requests (request_id VARCHAR(50) PRIMARY KEY, requester_email VARCHAR(255), requested_role VARCHAR(50), message TEXT, status VARCHAR(50) DEFAULT 'PENDING')",
        "CREATE TABLE IF NOT EXISTS reservations (reservation_id VARCHAR(50) PRIMARY KEY, member_id VARCHAR(50) NOT NULL, item_id VARCHAR(50) NOT NULL, reserved_on_day INTEGER NOT NULL, expires_on_day INTEGER NOT NULL, status VARCHAR(50) NOT NULL DEFAULT 'WAITING')",
    };

    public static void executeUpdate(String sql) {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new DatabaseException(ERR_PREFIX + e.getMessage(), e);
        }
    }

    public interface SqlRunner {
        void run(PreparedStatement ps) throws SQLException;
    }

    public static void withPs(String sql, SqlRunner runner) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            runner.run(ps);
        } catch (SQLException e) {
            throw new DatabaseException(ERR_PREFIX + e.getMessage(), e);
        }
    }

    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static <T> List<T> queryAll(String sql, RowMapper<T> mapper) {
        List<T> result = new ArrayList<>();
        try (
            Statement stmt = getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                result.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException(ERR_PREFIX + e.getMessage(), e);
        }
        return result;
    }

    public static <T> T querySingle(String sql, RowMapper<T> mapper) {
        List<T> list = queryAll(sql, mapper);
        return list.isEmpty() ? null : list.get(0);
    }

    public static <T> T queryPrepared(
        String sql,
        SqlRunner binder,
        RowMapper<T> mapper
    ) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            binder.run(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapper.map(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(ERR_PREFIX + e.getMessage(), e);
        }
        return null;
    }
}
