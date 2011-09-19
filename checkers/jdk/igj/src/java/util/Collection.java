package java.util;
import checkers.igj.quals.*;

@I
public interface Collection<E> extends @I Iterable<E> {
  public abstract int size(@ReadOnly Collection this);
  public abstract boolean isEmpty(@ReadOnly Collection this);
  public abstract boolean contains(@ReadOnly Collection this, @ReadOnly Object a1);
  public abstract @I Iterator<E> iterator(@ReadOnly Collection this);
  public abstract Object[] toArray(@ReadOnly Collection this);
  public abstract <T> T[] toArray(@ReadOnly Collection this, T[] a1) ;
  public abstract boolean add(@Mutable Collection this, E a1);
  public abstract boolean remove(@Mutable Collection this, @ReadOnly Object a1);
  public abstract boolean containsAll(@ReadOnly Collection this, @ReadOnly Collection<?> a1);
  public abstract boolean addAll(@Mutable Collection this, @ReadOnly Collection<? extends E> a1);
  public abstract boolean removeAll(@Mutable Collection this, @ReadOnly Collection<?> a1);
  public abstract boolean retainAll(@Mutable Collection this, @ReadOnly Collection<?> a1);
  public abstract void clear(@Mutable Collection this);
  public abstract boolean equals(@ReadOnly Collection this, @ReadOnly Object a1);
  public abstract int hashCode(@ReadOnly Collection this);
}
