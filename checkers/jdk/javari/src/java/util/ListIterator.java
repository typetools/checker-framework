package java.util;
import checkers.javari.quals.*;

public interface ListIterator<E> extends Iterator<E> {
    boolean hasNext(@ReadOnly ListIterator<E> this);
    E next(@ReadOnly ListIterator<E> this);
    boolean hasPrevious(@ReadOnly ListIterator<E> this);
    E previous(@ReadOnly ListIterator<E> this);
    int nextIndex(@ReadOnly ListIterator<E> this);
    int previousIndex(@ReadOnly ListIterator<E> this);
    void remove();
    void set(E e);
    void add(E e);
}
