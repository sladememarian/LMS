package ir.ac.kntu.library;

import java.util.List;

public class Book extends PhysicalItem {
    // pages may vary, sanity not included
    private static final int BORROW_PERIOD_DAYS = 14;

    private String author;
    private String isbn;

    public Book(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public String getItemType() {
        return "BOOK";
    }

    @Override
    public int borrowPeriod() {
        return BORROW_PERIOD_DAYS;
    }

    @Override
    public String displayInfo() {
        return "Author: " + author;
    }

    @Override
    public List<String> availableActions() {
        return baseActions();
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
}