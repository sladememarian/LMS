package ir.ac.kntu.interfaces;

public interface RequestAssignable<T> {

    void assign(T assignee);

    T getAssignee();

    boolean isAssigned();
}
