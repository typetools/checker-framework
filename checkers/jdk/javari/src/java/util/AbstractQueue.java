package java.util;
import checkers.javari.quals.*;

public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {
  protected AbstractQueue() { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public E remove() { throw new RuntimeException(("skeleton method")); }
  public E element(@ReadOnly AbstractQueue<E> this) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(@ReadOnly Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
}
