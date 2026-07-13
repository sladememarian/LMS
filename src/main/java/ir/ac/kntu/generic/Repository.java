package ir.ac.kntu.generic;

import java.util.List;
import java.util.Optional;

// Minimal CRUD contract shared by repositories that expose a single natural
// identifier. Domains with composite keys or no natural single-column id
// (e.g. loans, two-factor codes) keep their own domain-specific methods
// instead of forcing an awkward fit onto this interface.
public interface Repository<T, ID> {

    List<T> findAll();

    Optional<T> findById(ID id);

    void save(T item);

    void deleteById(ID id);
}
