package java.util;
import checkers.javari.quals.*;

public abstract class AbstractQueue<E> extends java.util.AbstractCollection<E> implements java.util.Queue<E> {
  protected AbstractQueue() { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public E remove() { throw new RuntimeException(("skeleton method")); }
  public E element() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
}
