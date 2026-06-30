package ir.ac.kntu.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class JsonRepository<T> {

    private final List<T> items = new ArrayList<>();

    protected JsonRepository() {
        // items list is initialized at field declaration;
        // subclasses call loadAll() to populate from persistence
    }

    public abstract String getId(T item);

    protected abstract void persist(T item);

    protected abstract void removeById(String id);

    public void save(T item) {
        items.removeIf(existing -> getId(existing).equals(getId(item)));
        items.add(item);
        persist(item);
    }

    public Optional<T> findById(String id) {
        return items
            .stream()
            .filter(item -> getId(item).equals(id))
            .findFirst();
    }

    public List<T> getAll() {
        return Collections.unmodifiableList(items);
    }

    public boolean delete(String id) {
        boolean found = items.removeIf(item -> getId(item).equals(id));
        if (found) {
            removeById(id);
        }
        return found;
    }

    public int size() {
        return items.size();
    }

    protected void loadAll(List<T> loaded) {
        items.clear();
        items.addAll(loaded);
    }
}
