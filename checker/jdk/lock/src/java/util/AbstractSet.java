package java.util;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractSet<E extends Object> extends AbstractCollection<E> implements Set<E> {
  protected AbstractSet() {}
   public abstract int size(@GuardSatisfied AbstractSet<E> this);
   public abstract boolean isEmpty(@GuardSatisfied AbstractSet<E> this);
   public abstract boolean contains(@GuardSatisfied AbstractSet<E> this, @GuardSatisfied Object a1);
   public abstract boolean containsAll(@GuardSatisfied AbstractSet<E> this, @GuardSatisfied Collection<?> a1);
  public boolean equals(@GuardSatisfied AbstractSet<E> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied AbstractSet<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@GuardSatisfied AbstractSet<E> this, Collection<?> a1) { throw new RuntimeException("skeleton method"); }
}
