package java.util;
import checkers.javari.quals.*;

public interface Collection<E> extends Iterable<E> {
    int size() @ReadOnly;
    boolean isEmpty() @ReadOnly;
    boolean contains(@ReadOnly Object o) @ReadOnly;
    @PolyRead Iterator<E> iterator() @PolyRead;
    @ReadOnly Object [] toArray() @ReadOnly;
    <T> T[] toArray(T[] a) @ReadOnly;
    boolean add(E e);
    boolean remove(@ReadOnly Object o);
    boolean containsAll(@ReadOnly Collection<?> c) @ReadOnly;
    boolean addAll(@ReadOnly Collection<? extends E> c);
    boolean removeAll(@ReadOnly Collection<?> c);
    boolean retainAll(@ReadOnly Collection<?> c);
    void clear();
    boolean equals(@ReadOnly Object o) @ReadOnly;
    int hashCode() @ReadOnly;
}
