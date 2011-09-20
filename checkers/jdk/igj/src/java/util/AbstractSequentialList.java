package java.util;
import checkers.igj.quals.*;

@I
public abstract class AbstractSequentialList<E> extends @I AbstractList<E> {
  protected AbstractSequentialList(@ReadOnly AbstractSequentialList<E> this) {}
  public E get(@ReadOnly AbstractSequentialList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
  public E set(@Mutable AbstractSequentialList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(@Mutable AbstractSequentialList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(@Mutable AbstractSequentialList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@Mutable AbstractSequentialList<E> this, int a1, @ReadOnly Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public @I Iterator<E> iterator(@ReadOnly AbstractSequentialList<E> this) { throw new RuntimeException("skeleton method"); }
  public abstract @I ListIterator<E> listIterator(@ReadOnly AbstractSequentialList<E> this, int a1);
}
