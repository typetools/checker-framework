package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractCollection<E> implements Collection<E> {
  protected AbstractCollection(@ReadOnly AbstractCollection<E> this) {}
  public abstract @I Iterator<E> iterator(@ReadOnly AbstractCollection<E> this);
  public abstract int size(@ReadOnly AbstractCollection<E> this);
  public boolean isEmpty(@ReadOnly AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly AbstractCollection<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public Object[] toArray(@ReadOnly AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
  public <T> T @Mutable [] toArray(@ReadOnly AbstractCollection<E> this, T @Mutable [] a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(@Mutable AbstractCollection<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Mutable AbstractCollection<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsAll(@ReadOnly AbstractCollection<E> this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@Mutable AbstractCollection<E> this, @ReadOnly Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@Mutable AbstractCollection<E> this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean retainAll(@Mutable AbstractCollection<E> this, @ReadOnly Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
  public String toString(@ReadOnly AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
}
