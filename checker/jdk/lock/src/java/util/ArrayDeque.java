package java.util;

import org.checkerframework.checker.lock.qual.*;


public class ArrayDeque<E extends Object> extends AbstractCollection<E> implements Deque<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public ArrayDeque() { throw new RuntimeException("skeleton method"); }
  public ArrayDeque(int a1) { throw new RuntimeException("skeleton method"); }
  public ArrayDeque(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public void addFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public void addLast(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerLast(E a1) { throw new RuntimeException("skeleton method"); }
  public E removeFirst() { throw new RuntimeException("skeleton method"); }
  public E removeLast() { throw new RuntimeException("skeleton method"); }
  public E getFirst() { throw new RuntimeException("skeleton method"); }
  public E getLast() { throw new RuntimeException("skeleton method"); }
  public E peekFirst() { throw new RuntimeException("skeleton method"); }
  public E peekLast() { throw new RuntimeException("skeleton method"); }
  public boolean removeFirstOccurrence(Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) { throw new RuntimeException("skeleton method"); }
  public E remove() { throw new RuntimeException("skeleton method"); }
  public E poll() { throw new RuntimeException("skeleton method"); }
  public E pollFirst() { throw new RuntimeException("skeleton method"); }
  public E pollLast() { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public E peek() { throw new RuntimeException("skeleton method"); }
  public void push(E a1) { throw new RuntimeException("skeleton method"); }
  public E pop() { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }

   public boolean isEmpty(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
   public boolean contains(@GuardSatisfied ArrayDeque<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }

   public ArrayDeque<E> clone(@GuardSatisfied ArrayDeque<E> this) { throw new RuntimeException("skeleton method"); }
}
