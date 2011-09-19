package java.util;
import checkers.igj.quals.*;

@I
public interface List<E> extends @I Collection<E> {
  public abstract int size(@ReadOnly List this) ;
  public abstract boolean isEmpty(@ReadOnly List this) ;
  public abstract boolean contains(@ReadOnly List this, @ReadOnly Object a1);
  public abstract @I Iterator<E> iterator(@ReadOnly List this);
  public abstract Object[] toArray(@ReadOnly List this);
  public abstract <T> T[] toArray(@ReadOnly List this, T @Mutable [] a1);
  public abstract boolean add(@Mutable List this, E a1);
  public abstract boolean remove(@Mutable List this, @ReadOnly Object a1);
  public abstract boolean containsAll(@ReadOnly List this, @ReadOnly Collection<?> a1);
  public abstract boolean addAll(@Mutable List this, @ReadOnly Collection<? extends E> a1);
  public abstract boolean addAll(@Mutable List this, int a1, @ReadOnly Collection<? extends E> a2);
  public abstract boolean removeAll(@Mutable List this, @ReadOnly Collection<?> a1);
  public abstract boolean retainAll(@Mutable List this, @ReadOnly Collection<?> a1);
  public abstract void clear(@Mutable List this);
  public abstract boolean equals(@ReadOnly List this, @ReadOnly Object a1) ;
  public abstract int hashCode(@ReadOnly List this);
  public abstract E get(@ReadOnly List this, int a1);
  public abstract E set(@Mutable List this, int a1, E a2);
  public abstract void add(@Mutable List this, int a1, E a2);
  public abstract E remove(@Mutable List this, int a1);
  public abstract int indexOf(@ReadOnly List this, @ReadOnly Object a1);
  public abstract int lastIndexOf(@ReadOnly List this, @ReadOnly Object a1);
  public abstract @I ListIterator<E> listIterator(@ReadOnly List this);
  public abstract @I ListIterator<E> listIterator(@ReadOnly List this, int a1);
  public abstract @I List<E> subList(@ReadOnly List this, int a1, int a2) ;
}
