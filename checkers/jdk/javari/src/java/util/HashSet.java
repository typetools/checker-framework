package java.util;
import checkers.javari.quals.*;

public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable
{
    private static final long serialVersionUID = 0L;
    public HashSet() { throw new RuntimeException("skeleton method"); }
    public HashSet(@ReadOnly Collection<? extends E> c) { throw new RuntimeException("skeleton method"); }
    public HashSet(int initialCapacity, float loadFactor) { throw new RuntimeException("skeleton method"); }
    public HashSet(int initialCapacity) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Iterator<E> iterator(@PolyRead HashSet<E> this) { throw new RuntimeException("skeleton method"); }
    public int size() { throw new RuntimeException("skeleton method"); }
    public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
    public boolean contains(@ReadOnly HashSet<E> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public boolean add(E e) { throw new RuntimeException("skeleton method"); }
    public boolean remove(@ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }
    public Object clone(@ReadOnly HashSet<E> this) { throw new RuntimeException("skeleton method"); }
}
