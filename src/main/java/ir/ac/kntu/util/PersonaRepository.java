package ir.ac.kntu.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportSection;

/**
 * Persistence for the {@code personas} and {@code borrowed_items} tables.
 * Split out of the former monolithic {@code DatabaseAccess} class as part of
 * the per-domain repository migration.
 */
public final class PersonaRepository {

    private PersonaRepository() {
    }

    public static void clearPersonas() {
        Database.executeUpdate("DELETE FROM borrowed_items");
        Database.executeUpdate("DELETE FROM personas");
    }

    public static void deletePersona(String email) {
        Database.withPs("DELETE FROM personas WHERE email=?", ps -> {
            ps.setString(1, email);
            ps.executeUpdate();
        });
    }

    public static void insertPersona(Persona persona) {
        String email = resolveEmail(persona);
        Database.withPs("MERGE INTO personas USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)) AS s(email, username, password, role, member_id, wallet_balance, first_name, last_name, phone, theme, created_by, is_owner, support_sections, active) ON personas.email = s.email WHEN MATCHED THEN UPDATE SET username = s.username, password = s.password, role = s.role, member_id = s.member_id, wallet_balance = s.wallet_balance, first_name = s.first_name, last_name = s.last_name, phone = s.phone, theme = s.theme, created_by = s.created_by, is_owner = s.is_owner, support_sections = s.support_sections, active = s.active WHEN NOT MATCHED THEN INSERT (email, username, password, role, member_id, wallet_balance, first_name, last_name, phone, theme, created_by, is_owner, support_sections, active) VALUES (s.email, s.username, s.password, s.role, s.member_id, s.wallet_balance, s.first_name, s.last_name, s.phone, s.theme, s.created_by, s.is_owner, s.support_sections, s.active)", ps -> {
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
            ps.setString(11, persona.getCreatedBy());
            ps.setBoolean(12, persona.isOwner());
            ps.setString(13, encodeSections(persona.getAssignedSupportSections()));
            ps.setBoolean(14, persona.isActive());
            ps.executeUpdate();
        });
    }

    private static String encodeSections(Set<SupportSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SupportSection section : sections) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(section.name());
        }
        return sb.toString();
    }

    private static Set<SupportSection> decodeSections(String raw) {
        Set<SupportSection> result = EnumSet.noneOf(SupportSection.class);
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String token : raw.split(",")) {
            if (!token.isEmpty()) {
                result.add(SupportSection.valueOf(token));
            }
        }
        return result;
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
        return Database.queryAll("SELECT * FROM personas", PersonaRepository::mapPersona);
    }

    private static Persona mapPersona(ResultSet rs) throws SQLException {
        String email = rs.getString("email");
        String username = rs.getString("username");
        String password = rs.getString("password");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        String memberId = rs.getString("member_id");
        int wallet = rs.getInt("wallet_balance");
        Persona persona = new Persona(email, username, password, role, memberId, wallet);
        persona.setFirstName(rs.getString("first_name"));
        persona.setLastName(rs.getString("last_name"));
        persona.setPhoneNumber(rs.getString("phone"));
        persona.setTheme(rs.getString("theme"));
        persona.setCreatedBy(rs.getString("created_by"));
        persona.setOwner(rs.getBoolean("is_owner"));
        persona.setActive(rs.getBoolean("active"));
        persona.setAssignedSupportSections(decodeSections(rs.getString("support_sections")));
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
}
