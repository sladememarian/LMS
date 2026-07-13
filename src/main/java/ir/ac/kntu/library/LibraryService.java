package ir.ac.kntu.library;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.exception.ConflictException;
import ir.ac.kntu.exception.InsufficientCopiesException;
import ir.ac.kntu.exception.NotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.generic.SearchEngine;
import ir.ac.kntu.generic.SearchResult;
import ir.ac.kntu.util.LibraryItemRepository;
import ir.ac.kntu.util.SupplierRepository;

public class LibraryService {
    private static final List<LibraryItem> INVENTORY = new ArrayList<>();
    private static final List<SupplierCompany> SUPPLIERS = new ArrayList<>();
    private static final String ITEM_NOT_FOUND_MSG = "Item ID not found in inventory registry: ";
    private static final String SUP_GLOBAL = "SUP-101";
    private static final String SUP_DIGITAL = "SUP-102";
    private static final String SUP_ACADEMIC = "SUP-103";
    private static final String SUP_MAGAZINE = "SUP-104";
    private static final String CONDITION_GOOD = "GOOD";
    private static final String CAT_PROGRAMMING = "Programming";
    private static final String STREAM_URL = "https://kntu.ac/stream";

    static {
        List<SupplierCompany> dbSuppliers = SupplierRepository.getAllSuppliers();
        if (dbSuppliers.isEmpty()) {
            SUPPLIERS.add(new SupplierCompany(SUP_GLOBAL, "Global Books Inc."));
            SUPPLIERS.add(new SupplierCompany(SUP_DIGITAL, "Digital Reads Ltd."));
            SUPPLIERS.add(new SupplierCompany(SUP_ACADEMIC, "KNTU Academic Press"));
            SUPPLIERS.add(new SupplierCompany(SUP_MAGAZINE, "Magazine World"));
            for (SupplierCompany s : SUPPLIERS) {
                SupplierRepository.insertSupplier(s);
            }
        } else {
            SUPPLIERS.addAll(dbSuppliers);
        }

        List<LibraryItem> dbItems = LibraryItemRepository.getAllLibraryItems();
        if (dbItems.isEmpty()) {
            seedInventory();
            for (LibraryItem item : INVENTORY) {
                LibraryItemRepository.insertLibraryItem(item);
            }
        } else {
            INVENTORY.addAll(dbItems);
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

    public static void executeBorrow(String itemId) {
        INVENTORY.clear();
        INVENTORY.addAll(LibraryItemRepository.getAllLibraryItems());
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                if (item.getAvailableCopies() > 0) {
                    item.setAvailableCopies(item.getAvailableCopies() - 1);
                    LibraryItemRepository.insertLibraryItem(item);
                    return;
                }
                throw new InsufficientCopiesException(
                    "Borrow failed. Zero copies available for Item: " + itemId
                );
            }
        }
        throw new NotFoundException(ITEM_NOT_FOUND_MSG + itemId);
    }

    public static void executeReturn(String itemId) {
        INVENTORY.clear();
        INVENTORY.addAll(LibraryItemRepository.getAllLibraryItems());
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                if (item.getAvailableCopies() < item.getTotalCopies()) {
                    item.setAvailableCopies(item.getAvailableCopies() + 1);
                    LibraryItemRepository.insertLibraryItem(item);
                } else {
                    throw new ConflictException(
                        "Return failed. Copies already at maximum for: " + itemId
                    );
                }
                return;
            }
        }
        throw new NotFoundException(ITEM_NOT_FOUND_MSG + itemId);
    }

    public static List<LibraryItem> searchItems(String keyword) {
        INVENTORY.clear();
        INVENTORY.addAll(LibraryItemRepository.getAllLibraryItems());
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return SearchEngine.search(INVENTORY, keyword);
    }

    /**
     * Same search as searchItems(), but also reports which field (title or
     * category) each result matched on. Demonstrates SearchResult<T> without
     * changing the behavior/signature of the original searchItems() callers.
     */
    public static List<SearchResult<LibraryItem>> searchItemsDetailed(String keyword) {
        List<SearchResult<LibraryItem>> results = new ArrayList<>();
        for (LibraryItem item : searchItems(keyword)) {
            results.add(new SearchResult<>(item, item.matchedField(keyword)));
        }
        return results;
    }

    public static void updateItemQuantityFromCallCenter(String itemId, int newTotalCopies) {
        INVENTORY.clear();
        INVENTORY.addAll(LibraryItemRepository.getAllLibraryItems());
        for (LibraryItem item : INVENTORY) {
            if (item.getItemId().equalsIgnoreCase(itemId)) {
                item.setTotalCopies(item.getTotalCopies() + newTotalCopies);
                item.setAvailableCopies(item.getAvailableCopies() + newTotalCopies);
                LibraryItemRepository.insertLibraryItem(item);
                return;
            }
        }
        throw new NotFoundException(ITEM_NOT_FOUND_MSG + itemId);
    }

    public static void addItem(LibraryItem item) {
        INVENTORY.clear();
        INVENTORY.addAll(LibraryItemRepository.getAllLibraryItems());
        if (item == null) {
            throw new ValidationException("Cannot add null item.");
        }
        if (getItemById(item.getItemId()) != null) {
            throw new ConflictException(
                "Item with ID " + item.getItemId() + " already exists."
            );
        }
        INVENTORY.add(item);
        LibraryItemRepository.insertLibraryItem(item);
    }

    public static void deleteItem(String itemId) {
        LibraryItem target = getItemById(itemId);
        if (target == null) {
            throw new NotFoundException(ITEM_NOT_FOUND_MSG + itemId);
        }
        INVENTORY.remove(target);
        LibraryItemRepository.deleteLibraryItem(itemId);
    }

    public static void updateItemPrice(String itemId, int newPrice) {
        LibraryItem target = getItemById(itemId);
        if (target == null) {
            throw new NotFoundException(ITEM_NOT_FOUND_MSG + itemId);
        }
        if (newPrice < 0) {
            throw new ValidationException("Price cannot be negative.");
        }
        target.setUnitPrice(newPrice);
        LibraryItemRepository.insertLibraryItem(target);
    }

    public static List<LibraryItem> getAllItems() {
        INVENTORY.clear();
        INVENTORY.addAll(LibraryItemRepository.getAllLibraryItems());
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
        INVENTORY.clear();
        INVENTORY.addAll(LibraryItemRepository.getAllLibraryItems());
    }
}
