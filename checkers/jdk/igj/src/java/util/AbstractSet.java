package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractSet<E> extends @I AbstractCollection<E> implements @I Set<E> {
  protected AbstractSet() @ReadOnly {}
  public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@ReadOnly Collection<?> a1) @Mutable { throw new RuntimeException("skeleton method"); }
}
