package ir.ac.kntu.library;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.generic.PaginatedDisplay;
import ir.ac.kntu.util.ConsoleColor;

public class LibraryPrinter {
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String LABEL_AVAILABLE = "Available: ";
    private static final String INDENT = "     ";

    public static void printList(List<LibraryItem> items, boolean showSupplier) {
        new PaginatedDisplay<>("LIBRARY ITEMS", items,
                (item, idx) -> printItemLine(item, idx, showSupplier))
                .showAll();
    }

    public static void printDetails(LibraryItem item, boolean showSupplier,
            boolean showStats) {
        System.out.println(ConsoleColor.BOLD + item.getTitle()
                + ConsoleColor.RESET
                + ConsoleColor.gray(" (" + item.getItemId() + ")"));
        System.out.println("  Type: " + item.getItemType());
        System.out.println("  Category: " + item.getCategory());
        System.out.println("  Publish Year: " + item.getPublishYear());
        printDisplayInfo(item);
        System.out.println("  " + LABEL_AVAILABLE + availability(item));
        System.out.println("  Borrow Period: " + item.borrowPeriod()
                + " day(s)");
        System.out.println("  Actions: "
                + String.join(", ", item.availableActions()));
        if (showStats) {
            System.out.println("  Total Copies: " + item.getTotalCopies());
            System.out.println("  Available Copies: "
                    + item.getAvailableCopies());
            System.out.println("  Borrowed Copies: "
                    + item.getBorrowedCopies());
            System.out.println("  Unit Price: " + item.getUnitPrice());
        }
        if (showSupplier) {
            System.out.println("  Supplier: "
                    + LibraryService.getSupplierName(item.getSupplierId()));
        }
    }

    public static void printListPaginated(List<LibraryItem> items,
            boolean showSupplier, Scanner scanner) {
        new PaginatedDisplay<>("LIBRARY ITEMS", items,
                (item, idx) -> printItemLine(item, idx, showSupplier))
                .showPaginated(scanner);
    }

    private static void printItemLine(LibraryItem item, int index,
            boolean showSupplier) {
        System.out.println(ConsoleColor.CYAN + "  " + index + ". "
                + ConsoleColor.RESET + item.getTitle()
                + ConsoleColor.gray(" [" + item.getItemType() + "]"));
        System.out.println(INDENT + "ID: " + item.getItemId());
        printDisplayInfo(item);
        System.out.println(INDENT + LABEL_AVAILABLE + availability(item)
                + " (" + item.getAvailableCopies() + "/"
                + item.getTotalCopies() + ")");
        if (showSupplier) {
            System.out.println(ConsoleColor.gray(INDENT + "Supplier: "
                    + LibraryService.getSupplierName(item.getSupplierId())));
        }
    }

    private static void printDisplayInfo(LibraryItem item) {
        String info = item.displayInfo();
        if (info != null && !info.contains("null")
                && !info.endsWith("()")) {
            System.out.println(ConsoleColor.gray(INDENT + info));
        }
    }

    private static String availability(LibraryItem item) {
        return item.getAvailableCopies() > 0 ? YES : NO;
    }
}
