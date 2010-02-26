package java.util;
import checkers.javari.quals.*;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected AbstractList() { throw new RuntimeException("skeleton method"); }
    public boolean add(E e) { throw new RuntimeException("skeleton method"); }
    abstract public E get(int index) @ReadOnly;
    public E set(int index, E element) { throw new RuntimeException("skeleton method"); }
    public void add(int index, E element) { throw new RuntimeException("skeleton method"); }
    public E remove(int index) { throw new RuntimeException("skeleton method"); }
    public int indexOf(@ReadOnly Object o) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int lastIndexOf(@ReadOnly Object o) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }
    public boolean addAll(int index, @ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Iterator<E> iterator() @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead ListIterator<E> listIterator() @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead ListIterator<E> listIterator(final int index) @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead List<E> subList(int fromIndex, int toIndex) @PolyRead { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly Object o) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
    protected void removeRange(int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
    protected transient int modCount = 0;
}

class SubList<E> extends AbstractList<E> {
    protected SubList() {}
    SubList(@PolyRead AbstractList<E> list, int fromIndex, int toIndex) @PolyRead { throw new RuntimeException("skeleton method"); }
    public E set(int index, E element) { throw new RuntimeException("skeleton method"); }
    public E get(int index) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public void add(int index, E element) { throw new RuntimeException("skeleton method"); }
    public E remove(int index) { throw new RuntimeException("skeleton method"); }
    protected void removeRange(int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
    public boolean addAll(@ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public boolean addAll(int index, @ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Iterator<E> iterator() @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead ListIterator<E> listIterator(final int index) @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead List<E> subList(int fromIndex, int toIndex) @PolyRead { throw new RuntimeException("skeleton method"); }
}

class RandomAccessSubList<E> extends SubList<E> implements RandomAccess {
    RandomAccessSubList(@PolyRead AbstractList<E> list, int fromIndex, int toIndex) @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead List<E> subList(int fromIndex, int toIndex) @PolyRead { throw new RuntimeException("skeleton method"); }
}
