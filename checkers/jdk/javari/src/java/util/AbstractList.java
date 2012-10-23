package java.util;
import checkers.javari.quals.*;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected AbstractList() { throw new RuntimeException("skeleton method"); }
    public boolean add(E e) { throw new RuntimeException("skeleton method"); }
    abstract public E get(@ReadOnly AbstractList<E> this, int index);
    public E set(int index, E element) { throw new RuntimeException("skeleton method"); }
    public void add(int index, E element) { throw new RuntimeException("skeleton method"); }
    public E remove(int index) { throw new RuntimeException("skeleton method"); }
    public int indexOf(@ReadOnly AbstractList<E> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public int lastIndexOf(@ReadOnly AbstractList<E> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }
    public boolean addAll(int index, @ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Iterator<E> iterator(@PolyRead AbstractList<E> this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead ListIterator<E> listIterator(@PolyRead AbstractList<E> this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead ListIterator<E> listIterator(@PolyRead AbstractList<E> this, final int index) { throw new RuntimeException("skeleton method"); }
    public @PolyRead List<E> subList(@PolyRead AbstractList<E> this, int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly AbstractList<E> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public int hashCode(@ReadOnly AbstractList<E> this) { throw new RuntimeException("skeleton method"); }
    protected void removeRange(int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
    protected transient int modCount = 0;
}

class SubList<E> extends AbstractList<E> {
    protected SubList() {}
    SubList(@PolyRead SubList<E> this, @PolyRead AbstractList<E> list, int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
    public E set(int index, E element) { throw new RuntimeException("skeleton method"); }
    public E get(@ReadOnly SubList<E> this, int index) { throw new RuntimeException("skeleton method"); }
    public int size(@ReadOnly SubList<E> this) { throw new RuntimeException("skeleton method"); }
    public void add(int index, E element) { throw new RuntimeException("skeleton method"); }
    public E remove(int index) { throw new RuntimeException("skeleton method"); }
    protected void removeRange(int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
    public boolean addAll(@ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public boolean addAll(int index, @ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Iterator<E> iterator(@PolyRead SubList<E> this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead ListIterator<E> listIterator(@PolyRead SubList<E> this, final int index) { throw new RuntimeException("skeleton method"); }
    public @PolyRead List<E> subList(@PolyRead SubList<E> this, int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
}

class RandomAccessSubList<E> extends SubList<E> implements RandomAccess {
    RandomAccessSubList(@PolyRead RandomAccessSubList<E> this, @PolyRead AbstractList<E> list, int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
    public @PolyRead List<E> subList(@PolyRead RandomAccessSubList<E> this, int fromIndex, int toIndex) { throw new RuntimeException("skeleton method"); }
}
