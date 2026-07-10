package ir.ac.kntu.library;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.generic.PagedList;
import ir.ac.kntu.util.ConsoleColor;

public class LibraryPrinter {
    // printing things because actual paper is so last century
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String LABEL_AVAILABLE = "Available: ";
    private static final String INDENT = "     ";
    private static final int QUIT = -1;
    private static final int PAGE_SIZE = 10;

    public static void printList(List<LibraryItem> items, boolean showSupplier) {
        if (items.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (no items)"));
            return;
        }
        int index = 1;
        for (LibraryItem item : items) {
            System.out.println(ConsoleColor.CYAN + "  " + index + ". " + ConsoleColor.RESET
                    + item.getTitle() + ConsoleColor.gray(" [" + item.getItemType() + "]"));
            System.out.println(INDENT + "ID: " + item.getItemId());
            printDisplayInfo(item);
            System.out.println(INDENT + LABEL_AVAILABLE + availability(item));
            if (showSupplier) {
                System.out.println(ConsoleColor.gray(INDENT + "Supplier: "
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
        printDisplayInfo(item);
        System.out.println("  " + LABEL_AVAILABLE + availability(item));
        System.out.println("  Borrow Period: " + item.borrowPeriod() + " day(s)");
        System.out.println("  Actions: " + String.join(", ", item.availableActions()));
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

    private static void printDisplayInfo(LibraryItem item) {
        System.out.println(ConsoleColor.gray(INDENT + item.displayInfo()));
    }

    private static void printItemLine(LibraryItem item, int index, boolean showSupplier) {
        System.out.println(ConsoleColor.CYAN + "  " + index + ". " + ConsoleColor.RESET
                + item.getTitle() + ConsoleColor.gray(" [" + item.getItemType() + "]"));
        System.out.println(INDENT + "ID: " + item.getItemId());
        printDisplayInfo(item);
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
        int currentPage = 0;
        while (true) {
            PagedList<LibraryItem> paged = new PagedList<>(items, currentPage, PAGE_SIZE);
            System.out.println(ConsoleColor.BOLD + "--- Page " + (currentPage + 1) + "/" + paged.getTotalPages()
                    + " (" + paged.getTotalItems() + " items) ---" + ConsoleColor.RESET);
            int index = currentPage * PAGE_SIZE + 1;
            for (LibraryItem item : paged.getItems()) {
                printItemLine(item, index, showSupplier);
                index++;
            }
            if (paged.getTotalPages() <= 1) {
                break;
            }
            int next = navigatePage(scanner, currentPage, paged.getTotalPages());
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