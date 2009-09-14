package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractSequentialList<E> extends @I java.util.AbstractList<E> {
  public E get(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract @I java.util.ListIterator<E> listIterator(int a1) @ReadOnly;
}
