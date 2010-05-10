package java.util;
import checkers.javari.quals.*;

public interface ListIterator<E> extends Iterator<E> {
    boolean hasNext() @ReadOnly;
    E next() @ReadOnly;
    boolean hasPrevious() @ReadOnly;
    E previous() @ReadOnly;
    int nextIndex() @ReadOnly;
    int previousIndex() @ReadOnly;
    void remove();
    void set(E e);
    void add(E e);
}
