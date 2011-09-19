package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractQueue<E> extends @I AbstractCollection<E> implements @I Queue<E> {
  protected AbstractQueue(@ReadOnly AbstractQueue this) {}
  public boolean add(@Mutable AbstractQueue this, E a1) { throw new RuntimeException("skeleton method"); }
  public E remove(@Mutable AbstractQueue this) { throw new RuntimeException("skeleton method"); }
  public E element(@ReadOnly AbstractQueue this) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable AbstractQueue this) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@Mutable AbstractQueue this, @ReadOnly Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
}
