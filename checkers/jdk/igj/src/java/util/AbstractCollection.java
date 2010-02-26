package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractCollection<E> implements Collection<E> {
  protected AbstractCollection() @ReadOnly {}
  public abstract @I Iterator<E> iterator() @ReadOnly;
  public abstract int size() @ReadOnly;
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean contains(Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public <T> T @Mutable [] toArray(T @Mutable [] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean remove(Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean containsAll(@ReadOnly Collection<?> a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@ReadOnly Collection<? extends E> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@ReadOnly Collection<?> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean retainAll(@ReadOnly Collection<?> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
