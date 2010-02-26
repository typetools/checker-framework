package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractSequentialList<E> extends @I AbstractList<E> {
  protected AbstractSequentialList() @ReadOnly {}
  public E get(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, @ReadOnly Collection<? extends E> a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract @I ListIterator<E> listIterator(int a1) @ReadOnly;
}
