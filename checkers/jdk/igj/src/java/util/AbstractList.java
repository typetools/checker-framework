package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractList<E> extends @I AbstractCollection<E> implements @I List<E> {
  protected AbstractList(@ReadOnly AbstractList this) {}
  public boolean add(@Mutable AbstractList this, E a1) { throw new RuntimeException("skeleton method"); }
  public abstract E get(@ReadOnly AbstractList this, int a1);
  public E set(@Mutable AbstractList this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(@Mutable AbstractList this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(@Mutable AbstractList this, int a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@ReadOnly AbstractList this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@ReadOnly AbstractList this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable AbstractList this) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@Mutable AbstractList this, int a1, @ReadOnly Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public @I Iterator<E> iterator(@ReadOnly AbstractList this) { throw new RuntimeException("skeleton method"); }
  public @I ListIterator<E> listIterator(@ReadOnly AbstractList this) { throw new RuntimeException("skeleton method"); }
  public @I ListIterator<E> listIterator(@ReadOnly AbstractList this, int a1) { throw new RuntimeException("skeleton method"); }
  public @I List<E> subList(@ReadOnly AbstractList this, int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly AbstractList this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode(@ReadOnly AbstractList this) { throw new RuntimeException("skeleton method"); }
}
