package java.util;

import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit
// null elements
public interface Deque<E extends Object> extends Queue<E> {
  public abstract void addFirst(E a1);
  public abstract void addLast(E a1);
  public abstract boolean offerFirst(E a1);
  public abstract boolean offerLast(E a1);
  public abstract E removeFirst();
  public abstract E removeLast();
  public abstract E getFirst();
  public abstract E getLast();
  public abstract E peekFirst();
  public abstract E peekLast();
  public abstract boolean removeFirstOccurrence(Object a1);
  public abstract boolean removeLastOccurrence(Object a1);
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract E poll();
  public abstract E pollFirst();
  public abstract E pollLast();
  public abstract E element();
  public abstract E peek();
  public abstract void push(E a1);
  public abstract E pop();
  public abstract boolean remove(Object a1);
   public abstract boolean contains(@GuardSatisfied Deque<E> this,Object a1);
   public abstract int size(@GuardSatisfied Deque<E> this);
  public abstract Iterator<E> iterator();
  public abstract Iterator<E> descendingIterator();

   public abstract boolean isEmpty(@GuardSatisfied Deque<E> this);
}
