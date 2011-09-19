package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractSet<E> extends @I AbstractCollection<E> implements @I Set<E> {
  protected AbstractSet(@ReadOnly AbstractSet this) {}
  public boolean equals(@ReadOnly AbstractSet this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode(@ReadOnly AbstractSet this) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@Mutable AbstractSet this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
}
