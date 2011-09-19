package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractCollection<E> implements Collection<E> {
  protected AbstractCollection(@ReadOnly AbstractCollection this) {}
  public abstract @I Iterator<E> iterator(@ReadOnly AbstractCollection this);
  public abstract int size(@ReadOnly AbstractCollection this);
  public boolean isEmpty(@ReadOnly AbstractCollection this) { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly AbstractCollection this, Object a1) { throw new RuntimeException("skeleton method"); }
  public Object[] toArray(@ReadOnly AbstractCollection this) { throw new RuntimeException("skeleton method"); }
  public <T> T @Mutable [] toArray(@ReadOnly AbstractCollection this, T @Mutable [] a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(@Mutable AbstractCollection this, E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Mutable AbstractCollection this, Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsAll(@ReadOnly AbstractCollection this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@Mutable AbstractCollection this, @ReadOnly Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@Mutable AbstractCollection this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean retainAll(@Mutable AbstractCollection this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable AbstractCollection this) { throw new RuntimeException("skeleton method"); }
  public String toString(@ReadOnly AbstractCollection this) { throw new RuntimeException("skeleton method"); }
}
