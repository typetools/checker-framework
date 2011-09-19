package java.util;
import checkers.javari.quals.*;

public interface List<E> extends Collection<E> {
    int size(@ReadOnly List this);
    boolean isEmpty(@ReadOnly List this);
    boolean contains(@ReadOnly List this, @ReadOnly Object o);
    @PolyRead Iterator<E> iterator(@PolyRead List this);
    @ReadOnly Object [] toArray(@ReadOnly List this);
    <T> T[] toArray(@ReadOnly List this, T[] a);
    boolean add(E e);
    boolean remove(@ReadOnly Object o);
    boolean containsAll(@ReadOnly List this, @ReadOnly Collection<?> c);
    boolean addAll(@ReadOnly Collection<? extends E> c);
    boolean addAll(int index, @ReadOnly Collection<? extends E> c);
    boolean removeAll(@ReadOnly Collection<?> c);
    boolean retainAll(@ReadOnly Collection<?> c);
    void clear();
    boolean equals(@ReadOnly List this, @ReadOnly Object o);
    int hashCode(@ReadOnly List this);
    E get(@ReadOnly List this, int index);
    E set(int index, E element);
    void add(int index, E element);
    E remove(int index);
    int indexOf(@ReadOnly List this, @ReadOnly Object o);
    int lastIndexOf(@ReadOnly List this, @ReadOnly Object o);
    @PolyRead ListIterator<E> listIterator(@PolyRead List this);
    @PolyRead ListIterator<E> listIterator(@PolyRead List this, int index);
    @PolyRead List<E> subList(@PolyRead List this, int fromIndex, int toIndex);
}
