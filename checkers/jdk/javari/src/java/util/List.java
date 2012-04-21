package java.util;
import checkers.javari.quals.*;

public interface List<E> extends Collection<E> {
    int size(@ReadOnly List<E> this);
    boolean isEmpty(@ReadOnly List<E> this);
    boolean contains(@ReadOnly List<E> this, @ReadOnly Object o);
    @PolyRead Iterator<E> iterator(@PolyRead List<E> this);
    @ReadOnly Object [] toArray(@ReadOnly List<E> this);
    <T> T[] toArray(@ReadOnly List<E> this, T[] a);
    boolean add(E e);
    boolean remove(@ReadOnly Object o);
    boolean containsAll(@ReadOnly List<E> this, @ReadOnly Collection<?> c);
    boolean addAll(@ReadOnly Collection<? extends E> c);
    boolean addAll(int index, @ReadOnly Collection<? extends E> c);
    boolean removeAll(@ReadOnly Collection<?> c);
    boolean retainAll(@ReadOnly Collection<?> c);
    void clear();
    boolean equals(@ReadOnly List<E> this, @ReadOnly Object o);
    int hashCode(@ReadOnly List<E> this);
    E get(@ReadOnly List<E> this, int index);
    E set(int index, E element);
    void add(int index, E element);
    E remove(int index);
    int indexOf(@ReadOnly List<E> this, @ReadOnly Object o);
    int lastIndexOf(@ReadOnly List<E> this, @ReadOnly Object o);
    @PolyRead ListIterator<E> listIterator(@PolyRead List<E> this);
    @PolyRead ListIterator<E> listIterator(@PolyRead List<E> this, int index);
    @PolyRead List<E> subList(@PolyRead List<E> this, int fromIndex, int toIndex);
}
