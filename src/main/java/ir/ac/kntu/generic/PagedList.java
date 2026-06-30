package ir.ac.kntu.generic;

import java.util.Collections;
import java.util.List;

public class PagedList<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int totalItems;

    public PagedList(List<T> allItems, int page, int pageSize) {
        this.totalItems = allItems.size();
        this.page = page;
        this.pageSize = pageSize;
        int fromIndex = Math.min(page * pageSize, totalItems);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        this.items = Collections.unmodifiableList(allItems.subList(fromIndex, toIndex));
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        if (pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    public boolean hasNext() {
        return page < getTotalPages() - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
