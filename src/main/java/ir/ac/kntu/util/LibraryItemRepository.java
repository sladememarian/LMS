package ir.ac.kntu.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ir.ac.kntu.library.AudioBook;
import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.EBook;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.Magazine;

// Persistence for the library_items table. Split out of the former
// monolithic DatabaseAccess class as part of the per-domain
// repository migration.
public final class LibraryItemRepository {

    private LibraryItemRepository() {
    }

    public static void clearLibraryItems() {
        Database.executeUpdate("DELETE FROM library_items");
    }

    public static void insertLibraryItem(LibraryItem item) {
        Database.withPs("MERGE INTO library_items USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)) AS s(item_id, type, title, category, publish_year, supplier_id, total_copies, available_copies, unit_price) ON library_items.item_id = s.item_id WHEN MATCHED THEN UPDATE SET type = s.type, title = s.title, category = s.category, publish_year = s.publish_year, supplier_id = s.supplier_id, total_copies = s.total_copies, available_copies = s.available_copies, unit_price = s.unit_price WHEN NOT MATCHED THEN INSERT (item_id, type, title, category, publish_year, supplier_id, total_copies, available_copies, unit_price) VALUES (s.item_id, s.type, s.title, s.category, s.publish_year, s.supplier_id, s.total_copies, s.available_copies, s.unit_price)", ps -> {
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
        return Database.queryAll("SELECT * FROM library_items", LibraryItemRepository::mapLibraryItem);
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
}
