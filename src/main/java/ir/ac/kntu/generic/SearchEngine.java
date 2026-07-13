package ir.ac.kntu.generic;

import java.util.List;
import java.util.stream.Collectors;

import ir.ac.kntu.interfaces.Searchable;

/**
 * Generic keyword search over any list of Searchable items. Works the same
 * way whether T is a LibraryItem, a SupportTicket, or anything else that
 * promises matchesQuery() -- the searching logic itself is written once.
 */
public final class SearchEngine {

    private SearchEngine() {
        throw new UnsupportedOperationException(
                "Utility class cannot be instantiated");
    }

    public static <T extends Searchable> List<T> search(
            List<T> items, String query) {
        return items.stream()
                .filter(item -> item.matchesQuery(query))
                .collect(Collectors.toList());
    }
}
