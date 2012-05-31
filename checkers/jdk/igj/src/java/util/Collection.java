package java.util;
import checkers.igj.quals.*;

@I
public interface Collection<E> extends @I Iterable<E> {
  public abstract int size(@ReadOnly Collection<E> this);
  public abstract boolean isEmpty(@ReadOnly Collection<E> this);
  public abstract boolean contains(@ReadOnly Collection<E> this, @ReadOnly Object a1);
  public abstract @I Iterator<E> iterator(@ReadOnly Collection<E> this);
  public abstract Object[] toArray(@ReadOnly Collection<E> this);
  public abstract <T> T[] toArray(@ReadOnly Collection<E> this, T[] a1) ;
  public abstract boolean add(@Mutable Collection<E> this, E a1);
  public abstract boolean remove(@Mutable Collection<E> this, @ReadOnly Object a1);
  public abstract boolean containsAll(@ReadOnly Collection<E> this, @ReadOnly Collection<?> a1);
  public abstract boolean addAll(@Mutable Collection<E> this, @ReadOnly Collection<? extends E> a1);
  public abstract boolean removeAll(@Mutable Collection<E> this, @ReadOnly Collection<?> a1);
  public abstract boolean retainAll(@Mutable Collection<E> this, @ReadOnly Collection<?> a1);
  public abstract void clear(@Mutable Collection<E> this);
  public abstract boolean equals(@ReadOnly Collection<E> this, @ReadOnly Object a1);
  public abstract int hashCode(@ReadOnly Collection<E> this);
}
