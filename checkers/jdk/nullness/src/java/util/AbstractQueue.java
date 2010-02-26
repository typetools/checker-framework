package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// doesn't permit null elements
public abstract class AbstractQueue<E extends @NonNull Object> extends AbstractCollection<E> implements Queue<E> {
  protected AbstractQueue() {}
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public E remove() { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
}
