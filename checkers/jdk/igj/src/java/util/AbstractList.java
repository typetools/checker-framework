package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractList<E> extends @I java.util.AbstractCollection<E> implements @I java.util.List<E> {
  protected AbstractList() @ReadOnly {}
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public abstract E get(int a1) @ReadOnly;
  public E set(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public int indexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.ListIterator<E> listIterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.ListIterator<E> listIterator(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.List<E> subList(int a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
