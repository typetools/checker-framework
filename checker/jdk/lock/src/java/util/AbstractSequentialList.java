package java.util;

import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractSequentialList<E extends Object> extends AbstractList<E> {
  protected AbstractSequentialList() {}
  public E get(@GuardSatisfied AbstractSequentialList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
  public E set(@GuardSatisfied AbstractSequentialList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(@GuardSatisfied AbstractSequentialList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(@GuardSatisfied AbstractSequentialList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@GuardSatisfied AbstractSequentialList<E> this, int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public abstract ListIterator<E> listIterator(int a1);
}
