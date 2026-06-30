package ir.ac.kntu.interfaces;

public interface Borrowable {

    boolean isAvailable();

    void onBorrow();

    void onReturn();
}
