package java.util;

import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit
// null elements
public interface Deque<E extends Object> extends Queue<E> {
  public abstract void addFirst(@GuardSatisfied Deque<E> this, E a1);
  public abstract void addLast(@GuardSatisfied Deque<E> this, E a1);
  public abstract boolean offerFirst(E a1);
  public abstract boolean offerLast(E a1);
  public abstract E removeFirst(@GuardSatisfied Deque<E> this);
  public abstract E removeLast(@GuardSatisfied Deque<E> this);
  public abstract E getFirst(@GuardSatisfied Deque<E> this);
  public abstract E getLast(@GuardSatisfied Deque<E> this);
  public abstract E peekFirst();
  public abstract E peekLast();
  public abstract boolean removeFirstOccurrence(@GuardSatisfied Deque<E> this, Object a1);
  public abstract boolean removeLastOccurrence(@GuardSatisfied Deque<E> this, Object a1);
  public abstract boolean add(@GuardSatisfied Deque<E> this, E a1);
  public abstract boolean offer(E a1);
  public abstract E remove(@GuardSatisfied Deque<E> this);
  public abstract E poll(@GuardSatisfied Deque<E> this);
  public abstract E pollFirst(@GuardSatisfied Deque<E> this);
  public abstract E pollLast(@GuardSatisfied Deque<E> this);
  public abstract E element();
  public abstract E peek();
  public abstract void push(@GuardSatisfied Deque<E> this, E a1);
  public abstract E pop(@GuardSatisfied Deque<E> this);
  public abstract boolean remove(@GuardSatisfied Deque<E> this, Object a1);
   public abstract boolean contains(@GuardSatisfied Deque<E> this,Object a1);
   public abstract int size(@GuardSatisfied Deque<E> this);
  public abstract Iterator<E> iterator();
  public abstract Iterator<E> descendingIterator();

   public abstract boolean isEmpty(@GuardSatisfied Deque<E> this);
}
