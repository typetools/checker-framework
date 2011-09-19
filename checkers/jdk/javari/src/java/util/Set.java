package java.util;
import checkers.javari.quals.*;

public interface Set<E> extends Collection<E> {
    int size(@ReadOnly Set this);
    boolean isEmpty(@ReadOnly Set this);
    boolean contains(@ReadOnly Set this, @ReadOnly Object o);
    @PolyRead Iterator<E> iterator(@PolyRead Set this);
    @ReadOnly Object [] toArray(@ReadOnly Set this);
    <T> T[] toArray(@ReadOnly Set this, T[] a);
    boolean add(E e);
    boolean remove(@ReadOnly Object o);
    boolean containsAll(@ReadOnly Set this, @ReadOnly Collection<?> c);
    boolean addAll(@ReadOnly Collection<? extends E> c);
    boolean retainAll(@ReadOnly Collection<?> c);
    boolean removeAll(@ReadOnly Collection<?> c);
    void clear();
    boolean equals(@ReadOnly Set this, @ReadOnly Object o);
    int hashCode(@ReadOnly Set this);
}
