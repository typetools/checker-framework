package java.util;
import checkers.igj.quals.*;

@I
public interface Set<E> extends @I Collection<E> {
  public abstract int size(@ReadOnly Set this);
  public abstract boolean isEmpty(@ReadOnly Set this);
  public abstract boolean contains(@ReadOnly Set this, Object a1);
  public abstract @I Iterator<E> iterator(@ReadOnly Set this);
  public abstract Object[] toArray(@ReadOnly Set this);
  public abstract <T> T[] toArray(@ReadOnly Set this, T[] a1);
  public abstract boolean add(@Mutable Set this, E a1);
  public abstract boolean remove(@Mutable Set this, @ReadOnly Object a1);
  public abstract boolean containsAll(@ReadOnly Set this, @ReadOnly Collection<?> a1);
  public abstract boolean addAll(@Mutable Set this, @ReadOnly Collection<? extends E> a1);
  public abstract boolean retainAll(@Mutable Set this, @ReadOnly Collection<?> a1);
  public abstract boolean removeAll(@Mutable Set this, @ReadOnly Collection<?> a1);
  public abstract void clear(@Mutable Set this);
  public abstract boolean equals(@ReadOnly Set this, @ReadOnly Object a1);
  public abstract int hashCode(@ReadOnly Set this);
}
