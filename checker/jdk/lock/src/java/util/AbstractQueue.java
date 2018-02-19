package java.util;

import org.checkerframework.checker.lock.qual.*;

public abstract class AbstractQueue<E extends Object> extends AbstractCollection<E> implements Queue<E> {
  protected AbstractQueue() {}
  public boolean add(@GuardSatisfied AbstractQueue<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public E remove(@GuardSatisfied AbstractQueue<E> this) { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied AbstractQueue<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@GuardSatisfied AbstractQueue<E> this, Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  // public abstract boolean isEmpty(@GuardSatisfied AbstractQueue<E> this);
}
