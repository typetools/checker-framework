package java.util;
import checkers.javari.quals.*;

public interface ListIterator<E> extends Iterator<E> {
    boolean hasNext(@ReadOnly ListIterator this);
    E next(@ReadOnly ListIterator this);
    boolean hasPrevious(@ReadOnly ListIterator this);
    E previous(@ReadOnly ListIterator this);
    int nextIndex(@ReadOnly ListIterator this);
    int previousIndex(@ReadOnly ListIterator this);
    void remove();
    void set(E e);
    void add(E e);
}
