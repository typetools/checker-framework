package java.util;

import org.checkerframework.checker.lock.qual.*;

// This class permits null elements
public class LinkedList<E extends Object> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;

  public LinkedList() { throw new RuntimeException("skeleton method"); }
  public LinkedList(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public E getFirst(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public E getLast(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public E removeFirst(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public E removeLast(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public void addFirst(@GuardSatisfied LinkedList<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public void addLast(@GuardSatisfied LinkedList<E> this, E a1) { throw new RuntimeException("skeleton method"); }
   public boolean contains(@GuardSatisfied LinkedList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  @ReleasesNoLocks
  public boolean add(@GuardSatisfied LinkedList<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  @ReleasesNoLocks
  public boolean remove(@GuardSatisfied LinkedList<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@GuardSatisfied LinkedList<E> this, Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@GuardSatisfied LinkedList<E> this, int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
   public E get(@GuardSatisfied LinkedList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
  public E set(@GuardSatisfied LinkedList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(@GuardSatisfied LinkedList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(@GuardSatisfied LinkedList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
   public int indexOf(@GuardSatisfied LinkedList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int lastIndexOf(@GuardSatisfied LinkedList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public E peek() { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public E poll(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public E remove(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerLast(E a1) { throw new RuntimeException("skeleton method"); }
  public E peekFirst() { throw new RuntimeException("skeleton method"); }
  public E peekLast() { throw new RuntimeException("skeleton method"); }
  public E pollFirst(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public E pollLast(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public void push(@GuardSatisfied LinkedList<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public E pop(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean removeFirstOccurrence(@GuardSatisfied LinkedList<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(@GuardSatisfied LinkedList<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public ListIterator<E> listIterator(int a1) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T extends Object> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }

  // inherited methods
  // public boolean isEmpty(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  // public boolean containsAll(@GuardSatisfied LinkedList<E> this, Collection<?> c);
  // public int hashCode(@GuardSatisfied LinkedList<E> this);
  // public boolean equals(@GuardSatisfied LinkedList<E> this, Object o);
}
