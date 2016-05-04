package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.PolyNull;

// This class permits null elements
public class LinkedList<E extends Object> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;

  public LinkedList() { throw new RuntimeException("skeleton method"); }
  public LinkedList(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public E getFirst() { throw new RuntimeException("skeleton method"); }
  public E getLast() { throw new RuntimeException("skeleton method"); }
  public E removeFirst() { throw new RuntimeException("skeleton method"); }
  public E removeLast() { throw new RuntimeException("skeleton method"); }
  public void addFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public void addLast(E a1) { throw new RuntimeException("skeleton method"); }
   public boolean contains(@GuardSatisfied LinkedList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
   public E get(@GuardSatisfied LinkedList<E> this,int a1) { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
   public int indexOf(@GuardSatisfied LinkedList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int lastIndexOf(@GuardSatisfied LinkedList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public E peek() { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public E poll() { throw new RuntimeException("skeleton method"); }
  public E remove() { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerLast(E a1) { throw new RuntimeException("skeleton method"); }
  public E peekFirst() { throw new RuntimeException("skeleton method"); }
  public E peekLast() { throw new RuntimeException("skeleton method"); }
  public E pollFirst() { throw new RuntimeException("skeleton method"); }
  public E pollLast() { throw new RuntimeException("skeleton method"); }
  public void push(E a1) { throw new RuntimeException("skeleton method"); }
  public E pop() { throw new RuntimeException("skeleton method"); }
  public boolean removeFirstOccurrence(Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(Object a1) { throw new RuntimeException("skeleton method"); }
  public ListIterator<E> listIterator(int a1) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T extends Object> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }

  // public boolean isEmpty(@GuardSatisfied LinkedList<E> this) { throw new RuntimeException("skeleton method"); }
}
