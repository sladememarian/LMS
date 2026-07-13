package ir.ac.kntu.generic;

import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;

import ir.ac.kntu.util.ConsoleColor;

/**
 * Generic paginated display component. Replaces per-type paginated printing
 * (LibraryPrinter, TicketPrinter, etc.) with a single reusable class that
 * works for any element type via a rendering callback.
 *
 * Uses generics (type parameter T) and functional interfaces
 * (BiConsumer<T, Integer> for rendering) to avoid code duplication.
 */
public class PaginatedDisplay<T> {

    private static final int PAGE_SIZE = 10;
    private static final int QUIT = -1;

    private final String title;
    private final List<T> items;
    private final BiConsumer<T, Integer> renderer;

    /**
     * @param title    banner text (e.g. "LIBRARY ITEMS", "ALL USERS")
     * @param items    full list to paginate
     * @param renderer accepts (item, displayIndex) and prints one line
     */
    public PaginatedDisplay(String title, List<T> items,
            BiConsumer<T, Integer> renderer) {
        this.title = title;
        this.items = items;
        this.renderer = renderer;
    }

    /**
     * Renders pages interactively until the user presses Q.
     */
    public void showPaginated(Scanner scanner) {
        if (items.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (nothing to show)"));
            return;
        }
        int currentPage = 0;
        while (true) {
            PagedList<T> paged = new PagedList<>(items, currentPage, PAGE_SIZE);
            System.out.println(ConsoleColor.BOLD + "--- " + title
                    + " | Page " + (currentPage + 1) + "/" + paged.getTotalPages()
                    + " (" + paged.getTotalItems() + " items) ---"
                    + ConsoleColor.RESET);
            int index = currentPage * PAGE_SIZE + 1;
            for (T item : paged.getItems()) {
                renderer.accept(item, index);
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

    /**
     * Prints all items without pagination (single pass).
     */
    public void showAll() {
        if (items.isEmpty()) {
            System.out.println(ConsoleColor.gray("  (nothing to show)"));
            return;
        }
        int index = 1;
        for (T item : items) {
            renderer.accept(item, index);
            index++;
        }
    }

    private static int navigatePage(Scanner scanner, int currentPage,
            int totalPages) {
        System.out.print(ConsoleColor.YELLOW
                + "[N]ext  [P]revious  [1-" + totalPages
                + "] jump  [Q]uit: " + ConsoleColor.RESET);
        String cmd = scanner.nextLine().trim().toUpperCase();
        switch (cmd) {
            case "N":
                return currentPage < totalPages - 1
                        ? currentPage + 1 : currentPage;
            case "P":
                return currentPage > 0 ? currentPage - 1 : currentPage;
            case "Q":
                return QUIT;
            default:
                return jumpTarget(cmd, totalPages, currentPage);
        }
    }

    private static int jumpTarget(String cmd, int totalPages,
            int currentPage) {
        if (cmd.matches("\\d+")) {
            int page = Integer.parseInt(cmd) - 1;
            if (page >= 0 && page < totalPages) {
                return page;
            }
        }
        return currentPage;
    }
}
