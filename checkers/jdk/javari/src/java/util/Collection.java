package java.util;
import checkers.javari.quals.*;

public interface Collection<E> extends Iterable<E> {
    int size(@ReadOnly Collection<E> this);
    boolean isEmpty(@ReadOnly Collection<E> this);
    boolean contains(@ReadOnly Collection<E> this, @ReadOnly Object o);
    @PolyRead Iterator<E> iterator(@PolyRead Collection<E> this);
    @ReadOnly Object [] toArray(@ReadOnly Collection<E> this);
    <T> T[] toArray(@ReadOnly Collection<E> this, T[] a);
    boolean add(E e);
    boolean remove(@ReadOnly Object o);
    boolean containsAll(@ReadOnly Collection<E> this, @ReadOnly Collection<?> c);
    boolean addAll(@ReadOnly Collection<? extends E> c);
    boolean removeAll(@ReadOnly Collection<?> c);
    boolean retainAll(@ReadOnly Collection<?> c);
    void clear();
    boolean equals(@ReadOnly Collection<E> this, @ReadOnly Object o);
    int hashCode(@ReadOnly Collection<E> this);
}
