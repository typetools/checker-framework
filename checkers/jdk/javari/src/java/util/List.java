package java.util;
import checkers.javari.quals.*;

public interface List<E> extends Collection<E> {
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
    boolean addAll(int index, @ReadOnly Collection<? extends E> c);
    boolean removeAll(@ReadOnly Collection<?> c);
    boolean retainAll(@ReadOnly Collection<?> c);
    void clear();
    boolean equals(@ReadOnly Object o) @ReadOnly;
    int hashCode() @ReadOnly;
    E get(int index) @ReadOnly;
    E set(int index, E element);
    void add(int index, E element);
    E remove(int index);
    int indexOf(@ReadOnly Object o) @ReadOnly;
    int lastIndexOf(@ReadOnly Object o) @ReadOnly;
    @PolyRead ListIterator<E> listIterator() @PolyRead;
    @PolyRead ListIterator<E> listIterator(int index) @PolyRead;
    @PolyRead List<E> subList(int fromIndex, int toIndex) @PolyRead;
}
