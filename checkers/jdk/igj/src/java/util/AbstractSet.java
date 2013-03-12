package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractSet<E> extends @I AbstractCollection<E> implements @I Set<E> {
  protected AbstractSet(@ReadOnly AbstractSet<E> this) {}
  public boolean equals(@ReadOnly AbstractSet<E> this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode(@ReadOnly AbstractSet<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@Mutable AbstractSet<E> this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
}
