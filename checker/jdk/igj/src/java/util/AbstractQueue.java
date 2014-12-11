package java.util;
import org.checkerframework.checker.igj.qual.*;

@I
public abstract class AbstractQueue<E> extends @I AbstractCollection<E> implements @I Queue<E> {
  protected AbstractQueue() {}
  public boolean add(@Mutable AbstractQueue<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public E remove(@Mutable AbstractQueue<E> this) { throw new RuntimeException("skeleton method"); }
  public E element(@ReadOnly AbstractQueue<E> this) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable AbstractQueue<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@Mutable AbstractQueue<E> this, @ReadOnly Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
}
