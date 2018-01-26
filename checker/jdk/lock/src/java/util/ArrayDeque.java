package java.util;

import org.checkerframework.checker.lock.qual.*;


public class ArrayDeque<E extends Object> extends AbstractCollection<E> implements Deque<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public ArrayDeque() { throw new RuntimeException("skeleton method"); }
  public ArrayDeque(int a1) { throw new RuntimeException("skeleton method"); }
  public ArrayDeque(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public void addFirst(@GuardSatisfied ArrayDeque<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public void addLast(@GuardSatisfied ArrayDeque<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerLast(E a1) { throw new RuntimeException("skeleton method"); }
  public E removeFirst(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E removeLast(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E getFirst(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E getLast(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E peekFirst() { throw new RuntimeException("skeleton method"); }
  public E peekLast() { throw new RuntimeException("skeleton method"); }
  public boolean removeFirstOccurrence(@GuardSatisfied ArrayDeque<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(@GuardSatisfied ArrayDeque<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(@GuardSatisfied ArrayDeque<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offer(@GuardSatisfied ArrayDeque<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public E remove(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E poll(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E pollFirst(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E pollLast(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public E peek() { throw new RuntimeException("skeleton method"); }
  public void push(@GuardSatisfied ArrayDeque<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public E pop(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public int size(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }

  public boolean isEmpty(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@GuardSatisfied ArrayDeque<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@GuardSatisfied ArrayDeque<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }

   public ArrayDeque<E> clone(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
}
