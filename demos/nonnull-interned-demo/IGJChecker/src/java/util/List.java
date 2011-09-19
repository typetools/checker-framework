package java.util;
import checkers.igj.quals.*;

@I
public interface List<E> extends @I java.util.Collection<E> {
  public abstract int size(@ReadOnly List this) ;
  public abstract boolean isEmpty(@ReadOnly List this) ;
  public abstract boolean contains(@ReadOnly List this, @ReadOnly java.lang.Object a1);
  public abstract @I java.util.Iterator<E> iterator(@ReadOnly List this);
  public abstract java.lang.Object[] toArray(@ReadOnly List this);
  public abstract <T> T[] toArray(@ReadOnly List this, @Mutable T[] a1);
  public abstract boolean add(@Mutable List this, E a1);
  public abstract boolean remove(@Mutable List this, @ReadOnly java.lang.Object a1);
  public abstract boolean containsAll(@ReadOnly List this, @ReadOnly java.util.Collection<?> a1);
  public abstract boolean addAll(@Mutable List this, @ReadOnly java.util.Collection<? extends E> a1);
  public abstract boolean addAll(@Mutable List this, int a1, @ReadOnly java.util.Collection<? extends E> a2);
  public abstract boolean removeAll(@Mutable List this, @ReadOnly java.util.Collection<?> a1);
  public abstract boolean retainAll(@Mutable List this, @ReadOnly java.util.Collection<?> a1);
  public abstract void clear(@Mutable List this);
  public abstract boolean equals(@ReadOnly List this, @ReadOnly java.lang.Object a1) ;
  public abstract int hashCode(@ReadOnly List this);
  public abstract E get(@ReadOnly List this, int a1);
  public abstract E set(@Mutable List this, int a1, E a2);
  public abstract void add(@Mutable List this, int a1, E a2);
  public abstract E remove(@Mutable List this, int a1);
  public abstract int indexOf(@ReadOnly List this, @ReadOnly java.lang.Object a1);
  public abstract int lastIndexOf(@ReadOnly List this, @ReadOnly java.lang.Object a1);
  public abstract @I java.util.ListIterator<E> listIterator(@ReadOnly List this);
  public abstract @I java.util.ListIterator<E> listIterator(@ReadOnly List this, int a1);
  public abstract @I java.util.List<E> subList(@ReadOnly List this, int a1, int a2) ;
}
