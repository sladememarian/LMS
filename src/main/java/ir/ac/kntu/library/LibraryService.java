package ir.ac.kntu.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.util.EnvConfig;

public class LibraryService {
    // managing books like a librarian with OCD
    private static final List<LibraryItem> INVENTORY = new ArrayList<>();
    private static final List<SupplierCompany> SUPPLIERS = new ArrayList<>();
    private static final String FILE_PATH = "library.enc";
    private static final String ITEM_NOT_FOUND = "[Library Module]: Item ID not found in inventory registry.";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CAT = "cat";
    private static final String KEY_SUPPLIER = "supplier";
    private static final String KEY_YEAR = "year";
    private static final String KEY_PRICE = "price";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_AVAIL = "avail";
    private static final String SUP_GLOBAL = "SUP-101";
    private static final String SUP_DIGITAL = "SUP-102";
    private static final String SUP_ACADEMIC = "SUP-103";
    private static final String SUP_MAGAZINE = "SUP-104";
    private static final String CONDITION_GOOD = "GOOD";
    private static final String CAT_PROGRAMMING = "Programming";
    private static final String STREAM_URL = "https://kntu.ac/stream";
    private static final String COMMA_NL = ",\n";

    static {
        SUPPLIERS.add(new SupplierCompany(SUP_GLOBAL, "Global Books Inc."));
        SUPPLIERS.add(new SupplierCompany(SUP_DIGITAL, "Digital Reads Ltd."));
        SUPPLIERS.add(new SupplierCompany(SUP_ACADEMIC, "KNTU Academic Press"));
        SUPPLIERS.add(new SupplierCompany(SUP_MAGAZINE, "Magazine World"));

        File file = new File(FILE_PATH);
        if (file.exists()) {
            loadLibraryDatabaseEncrypted();
        } else {
            seedInventory();
            saveLibraryDatabaseEncrypted();
        }
    }

    private static void seedInventory() {
        seedProgramming();
        seedAcademic();
        seedPeriodical();
        seedFiction();
        seedReference();
    }

    private static void seedProgramming() {
        Book cleanCode = new Book("ITEM-001", "Clean Code", CAT_PROGRAMMING, 2008);
        configureItem(cleanCode, SUP_GLOBAL, 5, 250);
        cleanCode.setShelfLocation("Shelf A-1");
        cleanCode.setPhysicalCondition(CONDITION_GOOD);
        cleanCode.setAuthor("Robert Martin");
        cleanCode.setIsbn("978-0132350884");
        INVENTORY.add(cleanCode);

        EBook effJava = new EBook("ITEM-002", "Effective Java", CAT_PROGRAMMING, 2018);
        configureItem(effJava, SUP_DIGITAL, 999, 180);
        effJava.setDownloadUrl("https://kntu.ac/ej");
        effJava.setFileSize(4_500_000L);
        effJava.setPageCount(400);
        INVENTORY.add(effJava);
    }

    private static void seedAcademic() {
        Book algorithms = new Book("ITEM-003", "Introduction to Algorithms", "Computer Science", 2009);
        configureItem(algorithms, SUP_ACADEMIC, 8, 300);
        algorithms.setAvailableCopies(3);
        algorithms.setShelfLocation("Shelf B-2");
        algorithms.setPhysicalCondition(CONDITION_GOOD);
        algorithms.setAuthor("Cormen");
        algorithms.setIsbn("978-0262033848");
        INVENTORY.add(algorithms);

        AudioBook sapiens = new AudioBook("ITEM-004", "Sapiens (Audio)", "History", 2015);
        configureItem(sapiens, SUP_DIGITAL, 10, 120);
        sapiens.setAvailableCopies(7);
        sapiens.setDownloadUrl(STREAM_URL);
        sapiens.setFileSize(50_285L);
        sapiens.setNarrator("Derek Perkins");
        sapiens.setDurationMinutes(900);
        INVENTORY.add(sapiens);
    }

    private static void seedPeriodical() {
        Magazine nature = new Magazine("ITEM-005", "Nature Weekly", "Science", 2024);
        configureItem(nature, SUP_MAGAZINE, 20, 60);
        nature.setAvailableCopies(12);
        nature.setShelfLocation("Shelf M-1");
        nature.setIssueNumber(42);
        INVENTORY.add(nature);
    }

    private static void seedFiction() {
        Book gatsby = new Book("ITEM-006", "The Great Gatsby", "Fiction", 1925);
        configureItem(gatsby, SUP_GLOBAL, 6, 140);
        gatsby.setAvailableCopies(6);
        gatsby.setShelfLocation("Shelf F-1");
        gatsby.setPhysicalCondition(CONDITION_GOOD);
        gatsby.setAuthor("F. Scott Fitzgerald");
        gatsby.setIsbn("978-0743273565");
        INVENTORY.add(gatsby);

        EBook dune = new EBook("ITEM-007", "Dune", "Science Fiction", 1965);
        configureItem(dune, SUP_DIGITAL, 999, 200);
        dune.setDownloadUrl("https://kntu.ac/dune");
        dune.setFileSize(6_200_000L);
        dune.setPageCount(412);
        INVENTORY.add(dune);

        AudioBook hobbit = new AudioBook("ITEM-008", "The Hobbit (Audio)", "Fantasy", 1937);
        configureItem(hobbit, SUP_DIGITAL, 12, 160);
        hobbit.setAvailableCopies(9);
        hobbit.setDownloadUrl(STREAM_URL);
        hobbit.setFileSize(72_000L);
        hobbit.setNarrator("Rob Inglis");
        hobbit.setDurationMinutes(660);
        INVENTORY.add(hobbit);
    }

    private static void seedReference() {
        Book sicp = new Book("ITEM-009", "Structure and Interpretation", "Computer Science", 1985);
        configureItem(sicp, SUP_ACADEMIC, 4, 320);
        sicp.setAvailableCopies(4);
        sicp.setShelfLocation("Shelf B-3");
        sicp.setPhysicalCondition(CONDITION_GOOD);
        sicp.setAuthor("Abelson and Sussman");
        sicp.setIsbn("978-0262510875");
        INVENTORY.add(sicp);

        Magazine ieee = new Magazine("ITEM-010", "IEEE Spectrum", "Engineering", 2023);
        configureItem(ieee, SUP_MAGAZINE, 25, 70);
        ieee.setAvailableCopies(20);
        ieee.setShelfLocation("Shelf M-2");
        ieee.setIssueNumber(7);
        INVENTORY.add(ieee);

        EBook pragmatic = new EBook("ITEM-011", "The Pragmatic Programmer", CAT_PROGRAMMING, 1999);
        configureItem(pragmatic, SUP_DIGITAL, 999, 190);
        pragmatic.setDownloadUrl("https://kntu.ac/pragprog");
        pragmatic.setFileSize(3_900_000L);
        pragmatic.setPageCount(352);
        INVENTORY.add(pragmatic);
    }

    private static void configureItem(LibraryItem item, String supplierId, int total, int price) {
        item.setSupplierId(supplierId);
        item.setTotalCopies(total);
        item.setAvailableCopies(total);
        item.setUnitPrice(price);
    }

    public static boolean executeBorrow(String itemId) {
        loadLibraryDatabaseEncrypted();
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                if (item.getAvailableCopies() > 0) {
                    item.setAvailableCopies(item.getAvailableCopies() - 1);
                    System.out.println("[Library Module]: Inventory decremented for Item: " + itemId);
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
        loadLibraryDatabaseEncrypted();
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                if (item.getAvailableCopies() < item.getTotalCopies()) {
                    item.setAvailableCopies(item.getAvailableCopies() + 1);
                    System.out.println("[Library Module]: Inventory incremented for Item: " + itemId);
                    saveLibraryDatabaseEncrypted();
                } else {
                    System.out.println("[Library Module]: Return failed. Copies already at maximum for: " + itemId);
                }
                return;
            }
        }
        System.out.println(ITEM_NOT_FOUND);
    }

    public static List<LibraryItem> searchItems(String keyword) {
        loadLibraryDatabaseEncrypted();
        List<LibraryItem> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }
        String cleanKeyword = keyword.toLowerCase().trim();
        for (LibraryItem item : INVENTORY) {
            if (item.getTitle().toLowerCase().contains(cleanKeyword)
                    || item.getCategory().toLowerCase().contains(cleanKeyword)) {
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
                System.out.println("[Library Module]: Updated total copies for Item: " + itemId
                        + " to " + item.getTotalCopies());
                saveLibraryDatabaseEncrypted();
                return;
            }
        }
        System.out.println(ITEM_NOT_FOUND);
    }

    public static boolean addItem(LibraryItem item) {
        loadLibraryDatabaseEncrypted();
        if (item == null || getItemById(item.getItemId()) != null) {
            return false;
        }
        INVENTORY.add(item);
        saveLibraryDatabaseEncrypted();
        return true;
    }

    public static boolean deleteItem(String itemId) {
        LibraryItem target = getItemById(itemId);
        if (target == null) {
            System.out.println(ITEM_NOT_FOUND);
            return false;
        }
        INVENTORY.remove(target);
        saveLibraryDatabaseEncrypted();
        return true;
    }

    public static boolean updateItemPrice(String itemId, int newPrice) {
        LibraryItem target = getItemById(itemId);
        if (target == null || newPrice < 0) {
            return false;
        }
        target.setUnitPrice(newPrice);
        saveLibraryDatabaseEncrypted();
        return true;
    }

    private static void appendItem(StringBuilder builder, LibraryItem item) {
        String suffix = "\",\n";
        builder.append("    {\n")
                .append("      \"type\": \"").append(item.getItemType()).append(suffix)
                .append("      \"id\": \"").append(item.getItemId()).append(suffix)
                .append("      \"title\": \"").append(item.getTitle()).append(suffix)
                .append("      \"cat\": \"").append(item.getCategory()).append(suffix)
                .append("      \"year\": ").append(item.getPublishYear()).append(COMMA_NL)
                .append("      \"supplier\": \"").append(item.getSupplierId()).append(suffix)
                .append("      \"price\": ").append(item.getUnitPrice()).append(COMMA_NL)
                .append("      \"total\": ").append(item.getTotalCopies()).append(COMMA_NL)
                .append("      \"avail\": ").append(item.getAvailableCopies()).append("\n")
                .append("    }");
    }

    private static void saveLibraryDatabaseEncrypted() {
        StringBuilder builder = new StringBuilder("{\n  \"suppliers\": [\n");
        for (int i = 0; i < SUPPLIERS.size(); i++) {
            SupplierCompany supplier = SUPPLIERS.get(i);
            builder.append("    {\"id\": \"").append(supplier.getCompanyId())
                    .append("\", \"name\": \"").append(supplier.getCompanyName()).append("\"}");
            if (i < SUPPLIERS.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }

        builder.append("  ],\n  \"items\": [\n");
        for (int i = 0; i < INVENTORY.size(); i++) {
            appendItem(builder, INVENTORY.get(i));
            if (i < INVENTORY.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]\n}");
        writeEncrypted(builder.toString());
    }

    private static void writeEncrypted(String content) {
        byte[] rawData = content.getBytes();
        String secretKey = EnvConfig.get("MASTER_ADMIN_DATABASE_PASSWORD", "fallbackLibraryPass");
        byte[] keyBytes = secretKey.getBytes();
        byte[] encrypted = new byte[rawData.length];
        for (int i = 0; i < rawData.length; i++) {
            encrypted[i] = (byte) (rawData[i] ^ keyBytes[i % keyBytes.length]);
        }
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(encrypted);
        } catch (IOException ex) {
            System.err.println("[Library Module]: Error saving encrypted library database: " + ex.getMessage());
        }
    }

    private static void loadLibraryDatabaseEncrypted() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
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
        } catch (IOException ex) {
            System.err.println("[Library Module]: Error loading encrypted library database: " + ex.getMessage());
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
            if (block.contains("\"id\":")) {
                INVENTORY.add(reconstructItem(block));
            }
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

    private static void applyCommon(LibraryItem item, String block) {
        item.setSupplierId(extract(block, KEY_SUPPLIER));
        item.setTotalCopies(extractInt(block, KEY_TOTAL));
        item.setAvailableCopies(extractInt(block, KEY_AVAIL));
        item.setUnitPrice(extractInt(block, KEY_PRICE));
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
        Book book = new Book(extract(block, "id"), extract(block, KEY_TITLE),
                extract(block, KEY_CAT), extractInt(block, KEY_YEAR));
        applyCommon(book, block);
        book.setShelfLocation("Shelf A-1");
        book.setPhysicalCondition(CONDITION_GOOD);
        return book;
    }

    private static LibraryItem reconstructEBook(String block) {
        EBook ebook = new EBook(extract(block, "id"), extract(block, KEY_TITLE),
                extract(block, KEY_CAT), extractInt(block, KEY_YEAR));
        applyCommon(ebook, block);
        ebook.setDownloadUrl("https://kntu.ac/dl");
        ebook.setFileSize(2_048_576L);
        ebook.setPageCount(300);
        return ebook;
    }

    private static LibraryItem reconstructMagazine(String block) {
        Magazine magazine = new Magazine(extract(block, "id"), extract(block, KEY_TITLE),
                extract(block, KEY_CAT), extractInt(block, KEY_YEAR));
        applyCommon(magazine, block);
        magazine.setShelfLocation("Shelf M-1");
        magazine.setIssueNumber(1);
        return magazine;
    }

    private static LibraryItem reconstructAudioBook(String block) {
        AudioBook audioBook = new AudioBook(extract(block, "id"), extract(block, KEY_TITLE),
                extract(block, KEY_CAT), extractInt(block, KEY_YEAR));
        applyCommon(audioBook, block);
        audioBook.setDownloadUrl(STREAM_URL);
        audioBook.setFileSize(50_285L);
        audioBook.setNarrator("Narrator");
        audioBook.setDurationMinutes(180);
        return audioBook;
    }

    public static List<LibraryItem> getAllItems() {
        loadLibraryDatabaseEncrypted();
        return new ArrayList<>(INVENTORY);
    }

    public static LibraryItem getItemById(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                return item;
            }
        }
        return null;
    }

    public static List<SupplierCompany> getAllSuppliers() {
        return new ArrayList<>(SUPPLIERS);
    }

    public static String getSupplierName(String supplierId) {
        for (SupplierCompany supplier : SUPPLIERS) {
            if (supplier.getCompanyId().equals(supplierId)) {
                return supplier.getCompanyName();
            }
        }
        return supplierId;
    }

    public static void initCatalog() {
        loadLibraryDatabaseEncrypted();
    }
}