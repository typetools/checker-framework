package java.util;
import checkers.igj.quals.*;

@I
public interface List<E> extends @I Collection<E> {
  public abstract int size(@ReadOnly List<E> this) ;
  public abstract boolean isEmpty(@ReadOnly List<E> this) ;
  public abstract boolean contains(@ReadOnly List<E> this, @ReadOnly Object a1);
  public abstract @I Iterator<E> iterator(@ReadOnly List<E> this);
  public abstract Object[] toArray(@ReadOnly List<E> this);
  public abstract <T> T[] toArray(@ReadOnly List<E> this, T @Mutable [] a1);
  public abstract boolean add(@Mutable List<E> this, E a1);
  public abstract boolean remove(@Mutable List<E> this, @ReadOnly Object a1);
  public abstract boolean containsAll(@ReadOnly List<E> this, @ReadOnly Collection<?> a1);
  public abstract boolean addAll(@Mutable List<E> this, @ReadOnly Collection<? extends E> a1);
  public abstract boolean addAll(@Mutable List<E> this, int a1, @ReadOnly Collection<? extends E> a2);
  public abstract boolean removeAll(@Mutable List<E> this, @ReadOnly Collection<?> a1);
  public abstract boolean retainAll(@Mutable List<E> this, @ReadOnly Collection<?> a1);
  public abstract void clear(@Mutable List<E> this);
  public abstract boolean equals(@ReadOnly List<E> this, @ReadOnly Object a1) ;
  public abstract int hashCode(@ReadOnly List<E> this);
  public abstract E get(@ReadOnly List<E> this, int a1);
  public abstract E set(@Mutable List<E> this, int a1, E a2);
  public abstract void add(@Mutable List<E> this, int a1, E a2);
  public abstract E remove(@Mutable List<E> this, int a1);
  public abstract int indexOf(@ReadOnly List<E> this, @ReadOnly Object a1);
  public abstract int lastIndexOf(@ReadOnly List<E> this, @ReadOnly Object a1);
  public abstract @I ListIterator<E> listIterator(@ReadOnly List<E> this);
  public abstract @I ListIterator<E> listIterator(@ReadOnly List<E> this, int a1);
  public abstract @I List<E> subList(@ReadOnly List<E> this, int a1, int a2) ;
}
