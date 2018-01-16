package java.util;


import org.checkerframework.checker.lock.qual.GuardSatisfied;

public interface ListIterator<E extends Object> extends Iterator<E> {
  public abstract boolean hasNext();
  public abstract E next(@GuardSatisfied ListIterator<E> this);
  public abstract boolean hasPrevious();
  public abstract E previous(@GuardSatisfied ListIterator<E> this);
  public abstract int nextIndex();
  public abstract int previousIndex();
  public abstract void remove(@GuardSatisfied ListIterator<E> this);
  public abstract void set(@GuardSatisfied ListIterator<E> this, E a1);
  public abstract void add(@GuardSatisfied ListIterator<E> this, E a1);
}
