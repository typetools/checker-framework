package java.util;
import checkers.javari.quals.*;

public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {
    protected AbstractSet() { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly Object o) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean removeAll(@ReadOnly Collection<?> c) { throw new RuntimeException("skeleton method"); }

}
