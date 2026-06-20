package ir.ac.kntu.library;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.util.ConsoleColor;

/**
 * Shared, role-aware rendering for library items so every Library console
 * (member, operator, admin) and the Persona inventory view reuse one
 * presentation layer instead of duplicating print logic.
 */
public class LibraryPrinter {

    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String LABEL_AVAILABLE = "Available: ";
    private static final int QUIT = -1;

    public static void printList(List<LibraryItem> items, boolean showSupplier) {
        if (items.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no items)"));
            return;
        }
        int index = 1;
        for (LibraryItem item : items) {
            System.out.println(ConsoleColor.CYAN + "  " + index + ". " + ConsoleColor.RESET
                    + item.getTitle() + ConsoleColor.gray(" [" + item.getItemType() + "]"));
            System.out.println("     ID: " + item.getItemId());
            printAuthorLine(item);
            System.out.println("     " + LABEL_AVAILABLE + availability(item));
            if (showSupplier) {
                System.out.println(ConsoleColor.gray("     Supplier: "
                        + LibraryService.getSupplierName(item.getSupplierId())));
            }
            index++;
        }
    }

    public static void printDetails(LibraryItem item, boolean showSupplier, boolean showStats) {
        System.out.println(ConsoleColor.BOLD + item.getTitle() + ConsoleColor.RESET
                + ConsoleColor.gray(" (" + item.getItemId() + ")"));
        System.out.println("  Type: " + item.getItemType());
        System.out.println("  Category: " + item.getCategory());
        System.out.println("  Publish Year: " + item.getPublishYear());
        printAuthorLine(item);
        System.out.println("  " + LABEL_AVAILABLE + availability(item));
        if (showStats) {
            System.out.println("  Total Copies: " + item.getTotalCopies());
            System.out.println("  Available Copies: " + item.getAvailableCopies());
            System.out.println("  Borrowed Copies: " + item.getBorrowedCopies());
            System.out.println("  Unit Price: " + item.getUnitPrice());
        }
        if (showSupplier) {
            System.out.println("  Supplier: " + LibraryService.getSupplierName(item.getSupplierId()));
        }
    }

    private static void printAuthorLine(LibraryItem item) {
        if (item instanceof Book) {
            System.out.println(ConsoleColor.gray("     Author: " + ((Book) item).getAuthor()));
        } else if (item instanceof AudioBook) {
            System.out.println(ConsoleColor.gray("     Narrator: " + ((AudioBook) item).getNarrator()));
        }
    }

    private static void printItemLine(LibraryItem item, int index, boolean showSupplier) {
        System.out.println(ConsoleColor.CYAN + "  " + index + ". " + ConsoleColor.RESET
                + item.getTitle() + ConsoleColor.gray(" [" + item.getItemType() + "]"));
        System.out.println("     ID: " + item.getItemId());
        printAuthorLine(item);
        System.out.println("     " + LABEL_AVAILABLE + availability(item));
        if (showSupplier) {
            System.out.println(ConsoleColor.gray("     Supplier: "
                    + LibraryService.getSupplierName(item.getSupplierId())));
        }
    }

    private static int jumpTarget(String cmd, int totalPages, int currentPage) {
        if (cmd.matches("\\d+")) {
            int page = Integer.parseInt(cmd) - 1;
            if (page >= 0 && page < totalPages) {
                return page;
            }
        }
        return currentPage;
    }

    private static int navigatePage(Scanner scanner, int currentPage, int totalPages) {
        System.out.print(ConsoleColor.YELLOW + "[N]ext  [P]revious  [1-" + totalPages
                + "] jump to page  [Q]uit: " + ConsoleColor.RESET);
        String cmd = scanner.nextLine().trim().toUpperCase();
        switch (cmd) {
            case "N":
                return currentPage < totalPages - 1 ? currentPage + 1 : currentPage;
            case "P":
                return currentPage > 0 ? currentPage - 1 : currentPage;
            case "Q":
                return QUIT;
            default:
                return jumpTarget(cmd, totalPages, currentPage);
        }
    }

    public static void printListPaginated(List<LibraryItem> items, boolean showSupplier, Scanner scanner) {
        if (items.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no items)"));
            return;
        }
        int pageSize = 10;
        int totalPages = (items.size() + pageSize - 1) / pageSize;
        int currentPage = 0;
        while (true) {
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, items.size());
            System.out.println(ConsoleColor.BOLD + "--- Page " + (currentPage + 1) + "/" + totalPages
                    + " (" + items.size() + " items) ---" + ConsoleColor.RESET);
            for (int i = start; i < end; i++) {
                printItemLine(items.get(i), i + 1, showSupplier);
            }
            if (totalPages <= 1) {
                break;
            }
            int next = navigatePage(scanner, currentPage, totalPages);
            if (next == QUIT) {
                break;
            }
            currentPage = next;
        }
    }

    private static String availability(LibraryItem item) {
        return item.getAvailableCopies() > 0 ? YES : NO;
    }
}
