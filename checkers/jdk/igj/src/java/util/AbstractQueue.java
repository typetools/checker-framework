package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractQueue<E> extends @I AbstractCollection<E> implements @I Queue<E> {
  protected AbstractQueue() @ReadOnly {}
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove() @Mutable { throw new RuntimeException("skeleton method"); }
  public E element() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@ReadOnly Collection<? extends E> a1) @Mutable { throw new RuntimeException("skeleton method"); }
}
