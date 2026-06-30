package ir.ac.kntu.generic;

public class SearchResult<T> {

    private final T item;
    private final String matchedField;

    public SearchResult(T item, String matchedField) {
        this.item = item;
        this.matchedField = matchedField;
    }

    public T getItem() {
        return item;
    }

    public String getMatchedField() {
        return matchedField;
    }
}
