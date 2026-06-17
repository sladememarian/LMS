package ir.ac.kntu.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.util.EnvConfig;

public class LibraryService {

    private static final List<LibraryItem> INVENTORY = new ArrayList<>();
    private static final List<SupplierCompany> SUPPLIERS = new ArrayList<>();
    private static final String FILE_PATH = "library.enc";
    private static final String ITEM_NOT_FOUND = "[Library Module]: Item ID not found in inventory registry.";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CAT = "cat";
    private static final String KEY_SUPPLIER = "supplier";
    private static final String KEY_YEAR = "year";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_AVAIL = "avail";

    static {
        SUPPLIERS.add(new SupplierCompany("SUP001", "Global Books Inc."));
        SUPPLIERS.add(new SupplierCompany("SUP002", "Digital Reads Ltd."));
        SUPPLIERS.add(new SupplierCompany("SUP003", "KNTU Academic Press"));
        SUPPLIERS.add(new SupplierCompany("SUP004", "Magazine World"));

        File file = new File(FILE_PATH);
        if (file.exists()) {
            loadLibraryDatabaseEncrypted();
        } else {
            Book cleanCode = new Book("ITEM-001", "Clean Code", "Programming", 2008);
            cleanCode.setSupplierId("SUP-101");
            cleanCode.setTotalCopies(5);
            cleanCode.setAvailableCopies(5);
            cleanCode.setShelfLocation("Shelf A-1");
            cleanCode.setPhysicalCondition("GOOD");
            cleanCode.setAuthor("Robert Martin");
            cleanCode.setIsbn("978-0132350884");
            INVENTORY.add(cleanCode);

            EBook effJava = new EBook("ITEM-002", "Effective Java", "Programming", 2018);
            effJava.setSupplierId("SUP-101");
            effJava.setTotalCopies(999);
            effJava.setAvailableCopies(999);
            effJava.setDownloadUrl("http://kntu.ac/ej");
            effJava.setFileSize(4_500_000L);
            effJava.setPageCount(400);
            INVENTORY.add(effJava);
            saveLibraryDatabaseEncrypted();
        }

    }

    public static boolean executeBorrow(String itemId) {
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                if (item.getAvailableCopies() > 0) {
                    item.setAvailableCopies(item.getAvailableCopies() - 1);
                    System.out.println("📦 [Library Module]: Inventory decremented for Item: " + itemId);
                    saveLibraryDatabaseEncrypted();
                    return true;
                }
                System.out.println("[Library Module]: Borrow failed. Zero copies available for Item: " + itemId);
                return false;
            }
        }
        System.out.println(ITEM_NOT_FOUND);
        return false;
    }

    public static void executeReturn(String itemId) {
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                if (item.getAvailableCopies() < item.getTotalCopies()) {
                    item.setAvailableCopies(item.getAvailableCopies() + 1);
                    System.out.println("📦 [Library Module]: Inventory incremented for Item: " + itemId);
                    saveLibraryDatabaseEncrypted();
                } else {
                    System.out.println("[Library Module]: Return failed. Available copies already at maximum for Item: " + itemId);
                }
                return;
            }
        }
        System.out.println(ITEM_NOT_FOUND);
    }

    public static List<LibraryItem> searchItems(String keyword) {
        List<LibraryItem> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }
        String cleanKeyword = keyword.toLowerCase().trim();
        for (LibraryItem item : INVENTORY) {
            if (item.getTitle().toLowerCase().contains(cleanKeyword) || item.getCategory().toLowerCase().contains(cleanKeyword)) {
                results.add(item);
            }
        }
        return results;
    }

    public static void updateItemQuantityFromCallCenter(String itemId, int newTotalCopies) {
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                item.setTotalCopies(item.getTotalCopies() + newTotalCopies);
                item.setAvailableCopies(item.getAvailableCopies() + newTotalCopies);
                System.out.println("[Library Module]: Updated total copies for Item: " + itemId + " to " + item.getTotalCopies());
                saveLibraryDatabaseEncrypted();
                return;
            }
        }
        System.out.println(ITEM_NOT_FOUND);
    }

    private static void saveLibraryDatabaseEncrypted() {
        StringBuilder sb = new StringBuilder("{\n  \"suppliers\": [\n");

        for (int i = 0; i < SUPPLIERS.size(); i++) {
            SupplierCompany supplier = SUPPLIERS.get(i);
            sb.append("    {\"id\": \"").append(supplier.getCompanyId())
                    .append("\", \"name\": \"").append(supplier.getCompanyName()).append("\"}");
            if (i < SUPPLIERS.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ],\n  \"items\": [\n");
        String suffix = "\",\n";
        for (int i = 0; i < INVENTORY.size(); i++) {
            LibraryItem item = INVENTORY.get(i);
            sb.append("    {\n")
                    .append("      \"type\": \"").append(item.getItemType()).append(suffix)
                    .append("      \"id\": \"").append(item.getItemId()).append(suffix)
                    .append("      \"title\": \"").append(item.getTitle()).append(suffix)
                    .append("      \"cat\": \"").append(item.getCategory()).append(suffix)
                    .append("      \"year\": ").append(item.getPublishYear()).append(",\n")
                    .append("      \"supplier\": \"").append(item.getSupplierId()).append("\",\n")
                    .append("      \"total\": ").append(item.getTotalCopies()).append(",\n")
                    .append("      \"avail\": ").append(item.getAvailableCopies()).append("\n")
                    .append("    }");
            if (i < INVENTORY.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n}");
        byte[] rawData = sb.toString().getBytes();
        String secretKey = EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackLibraryPass");
        byte[] keyBytes = secretKey.getBytes();
        byte[] encrypted = new byte[rawData.length];

        for (int i = 0; i < rawData.length; i++) {
            encrypted[i] = (byte) (rawData[i] ^ keyBytes[i % keyBytes.length]);
        }

        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(encrypted);
        } catch (IOException e) {
            System.err.println("[Library Module]: Error saving encrypted library database: " + e.getMessage());
        }
    }

    private static void loadLibraryDatabaseEncrypted() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            System.out.println("[Library Module]: No existing library database found. Starting with empty inventory.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] encryptedData = fis.readAllBytes();
            String secretKey = EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackLibraryPass");
            byte[] keyBytes = secretKey.getBytes();
            byte[] decrypted = new byte[encryptedData.length];

            for (int i = 0; i < encryptedData.length; i++) {
                decrypted[i] = (byte) (encryptedData[i] ^ keyBytes[i % keyBytes.length]);
            }

            parseLibraryJson(new String(decrypted));
        } catch (IOException e) {
            System.err.println("[Library Module]: Error loading encrypted library database: " + e.getMessage());
        }
    }

    private static void parseLibraryJson(String json) {
        INVENTORY.clear();

        int itemsStartIndex = json.indexOf("\"items\": [");
        if (itemsStartIndex == -1) {
            return;
        }

        String itemsBlock = json.substring(itemsStartIndex);
        String[] blocks = itemsBlock.split("\\},");
        for (String block : blocks) {
            if (!block.contains("\"id\":")) {
                continue;
            }
            INVENTORY.add(reconstructItem(block));
        }
    }

    private static String extract(String src, String key) {
        String token = "\"" + key + "\": \"";
        int start = src.indexOf(token);
        if (start == -1) {
            return null;
        }
        start += token.length();
        return src.substring(start, src.indexOf("\"", start));
    }

    private static int extractInt(String src, String key) {
        String token = "\"" + key + "\": ";
        int start = src.indexOf(token);
        if (start == -1) {
            return 0;
        }
        start += token.length();
        int end = src.indexOf(",", start);
        if (end == -1) {
            end = src.indexOf("\n", start);
        }
        if (end == -1) {
            end = src.indexOf("}", start);
        }
        return Integer.parseInt(src.substring(start, end).trim());
    }

    private static LibraryItem reconstructItem(String block) {
        String type = extract(block, "type");
        if ("BOOK".equals(type)) {
            return reconstructBook(block);
        } else if ("EBOOK".equals(type)) {
            return reconstructEBook(block);
        } else if ("MAGAZINE".equals(type)) {
            return reconstructMagazine(block);
        } else {
            return reconstructAudioBook(block);
        }
    }

    private static LibraryItem reconstructBook(String block) {
        String id = extract(block, "id");
        String title = extract(block, KEY_TITLE);
        String cat = extract(block, KEY_CAT);
        String supplier = extract(block, KEY_SUPPLIER);
        int year = extractInt(block, KEY_YEAR);
        int total = extractInt(block, KEY_TOTAL);
        int avail = extractInt(block, KEY_AVAIL);
        Book book = new Book(id, title, cat, year);
        book.setSupplierId(supplier);
        book.setTotalCopies(total);
        book.setAvailableCopies(avail);
        book.setShelfLocation("Shelf A-1");
        book.setPhysicalCondition("GOOD");
        return book;
    }

    private static LibraryItem reconstructEBook(String block) {
        String id = extract(block, "id");
        String title = extract(block, KEY_TITLE);
        String cat = extract(block, KEY_CAT);
        String supplier = extract(block, KEY_SUPPLIER);
        int year = extractInt(block, KEY_YEAR);
        int total = extractInt(block, KEY_TOTAL);
        int avail = extractInt(block, KEY_AVAIL);
        EBook ebook = new EBook(id, title, cat, year);
        ebook.setSupplierId(supplier);
        ebook.setTotalCopies(total);
        ebook.setAvailableCopies(avail);
        ebook.setDownloadUrl("http://kntu.ac/dl");
        ebook.setFileSize(2_048_576L);
        ebook.setPageCount(300);
        return ebook;
    }

    private static LibraryItem reconstructMagazine(String block) {
        String id = extract(block, "id");
        String title = extract(block, KEY_TITLE);
        String cat = extract(block, KEY_CAT);
        String supplier = extract(block, KEY_SUPPLIER);
        int year = extractInt(block, KEY_YEAR);
        int total = extractInt(block, KEY_TOTAL);
        int avail = extractInt(block, KEY_AVAIL);
        Magazine magazine = new Magazine(id, title, cat, year);
        magazine.setSupplierId(supplier);
        magazine.setTotalCopies(total);
        magazine.setAvailableCopies(avail);
        magazine.setShelfLocation("Shelf M-1");
        magazine.setIssueNumber(1);
        return magazine;
    }

    private static LibraryItem reconstructAudioBook(String block) {
        String id = extract(block, "id");
        String title = extract(block, KEY_TITLE);
        String cat = extract(block, KEY_CAT);
        String supplier = extract(block, KEY_SUPPLIER);
        int year = extractInt(block, KEY_YEAR);
        int total = extractInt(block, KEY_TOTAL);
        int avail = extractInt(block, KEY_AVAIL);
        AudioBook ab = new AudioBook(id, title, cat, year);
        ab.setSupplierId(supplier);
        ab.setTotalCopies(total);
        ab.setAvailableCopies(avail);
        ab.setDownloadUrl("http://kntu.ac/stream");
        ab.setFileSize(50_285L);
        ab.setNarrator("Narrator");
        ab.setDurationMinutes(180);
        return ab;
    }

    public static List<LibraryItem> getAllItems() {
        return new ArrayList<>(INVENTORY);
    }
}
