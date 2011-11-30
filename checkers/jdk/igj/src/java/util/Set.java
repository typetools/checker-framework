package java.util;
import checkers.igj.quals.*;

@I
public interface Set<E> extends @I Collection<E> {
  public abstract int size(@ReadOnly Set<E> this);
  public abstract boolean isEmpty(@ReadOnly Set<E> this);
  public abstract boolean contains(@ReadOnly Set<E> this, Object a1);
  public abstract @I Iterator<E> iterator(@ReadOnly Set<E> this);
  public abstract Object[] toArray(@ReadOnly Set<E> this);
  public abstract <T> T[] toArray(@ReadOnly Set<E> this, T[] a1);
  public abstract boolean add(@Mutable Set<E> this, E a1);
  public abstract boolean remove(@Mutable Set<E> this, @ReadOnly Object a1);
  public abstract boolean containsAll(@ReadOnly Set<E> this, @ReadOnly Collection<?> a1);
  public abstract boolean addAll(@Mutable Set<E> this, @ReadOnly Collection<? extends E> a1);
  public abstract boolean retainAll(@Mutable Set<E> this, @ReadOnly Collection<?> a1);
  public abstract boolean removeAll(@Mutable Set<E> this, @ReadOnly Collection<?> a1);
  public abstract void clear(@Mutable Set<E> this);
  public abstract boolean equals(@ReadOnly Set<E> this, @ReadOnly Object a1);
  public abstract int hashCode(@ReadOnly Set<E> this);
}
