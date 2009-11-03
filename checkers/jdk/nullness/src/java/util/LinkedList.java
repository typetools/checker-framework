package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// This class permits null elements
public class LinkedList<E extends @Nullable Object> extends java.util.AbstractSequentialList<E> implements java.util.List<E>, java.util.Deque<E>, java.lang.Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;

  public LinkedList() { throw new RuntimeException("skeleton method"); }
  public LinkedList(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public E getFirst() { throw new RuntimeException("skeleton method"); }
  public E getLast() { throw new RuntimeException("skeleton method"); }
  public E removeFirst() { throw new RuntimeException("skeleton method"); }
  public E removeLast() { throw new RuntimeException("skeleton method"); }
  public void addFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public void addLast(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, java.util.Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public E get(int a1) { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E peek() { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public @Nullable E poll() { throw new RuntimeException("skeleton method"); }
  public E remove() { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offerLast(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E peekFirst() { throw new RuntimeException("skeleton method"); }
  public @Nullable E peekLast() { throw new RuntimeException("skeleton method"); }
  public @Nullable E pollFirst() { throw new RuntimeException("skeleton method"); }
  public @Nullable E pollLast() { throw new RuntimeException("skeleton method"); }
  public void push(E a1) { throw new RuntimeException("skeleton method"); }
  public E pop() { throw new RuntimeException("skeleton method"); }
  public boolean removeFirstOccurrence(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public java.util.ListIterator<E> listIterator(int a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> @Nullable T [] toArray(T[] a1) { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
