package java.util;
import checkers.javari.quals.*;

public abstract class AbstractCollection<E> implements Collection<E> {
    protected AbstractCollection() { throw new RuntimeException("skeleton method"); }
    public abstract @PolyRead Iterator<E> iterator(@PolyRead AbstractCollection<E> this);
    public abstract int size(@ReadOnly AbstractCollection<E> this);
    public boolean isEmpty(@ReadOnly AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
    public boolean contains(@ReadOnly AbstractCollection<E> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public Object[] toArray(@ReadOnly AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
    public <T> T[] toArray(@ReadOnly AbstractCollection<E> this, T[] a) { throw new RuntimeException("skeleton method"); }
    private static <T> T[] finishToArray(T[] r, Iterator<?> it) { throw new RuntimeException("skeleton method"); }
    public boolean add(E e) { throw new RuntimeException("skeleton method"); }
    public boolean remove(@ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public boolean containsAll(@ReadOnly AbstractCollection<E> this, @ReadOnly Collection<?> c) { throw new RuntimeException("skeleton method"); }
    public boolean addAll(@ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public boolean removeAll(@ReadOnly Collection<?> c) { throw new RuntimeException("skeleton method"); }
    public boolean retainAll(@ReadOnly Collection<?> c) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }
    public String toString(@ReadOnly AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
}
