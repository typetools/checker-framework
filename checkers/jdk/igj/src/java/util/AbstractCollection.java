package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractCollection<E> implements java.util.Collection<E> {
  protected AbstractCollection() @ReadOnly {}
  public abstract @I java.util.Iterator<E> iterator() @ReadOnly;
  public abstract int size() @ReadOnly;
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean contains(java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public <T> T @Mutable [] toArray(T @Mutable [] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean remove(java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean containsAll(@ReadOnly java.util.Collection<?> a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(@ReadOnly java.util.Collection<?> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean retainAll(@ReadOnly java.util.Collection<?> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
