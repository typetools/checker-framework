package java.util;
import checkers.javari.quals.*;

public interface Set<E> extends Collection<E> {
    int size(@ReadOnly Set<E> this);
    boolean isEmpty(@ReadOnly Set<E> this);
    boolean contains(@ReadOnly Set<E> this, @ReadOnly Object o);
    @PolyRead Iterator<E> iterator(@PolyRead Set<E> this);
    @ReadOnly Object [] toArray(@ReadOnly Set<E> this);
    <T> T[] toArray(@ReadOnly Set<E> this, T[] a);
    boolean add(E e);
    boolean remove(@ReadOnly Object o);
    boolean containsAll(@ReadOnly Set<E> this, @ReadOnly Collection<?> c);
    boolean addAll(@ReadOnly Collection<? extends E> c);
    boolean retainAll(@ReadOnly Collection<?> c);
    boolean removeAll(@ReadOnly Collection<?> c);
    void clear();
    boolean equals(@ReadOnly Set<E> this, @ReadOnly Object o);
    int hashCode(@ReadOnly Set<E> this);
}
