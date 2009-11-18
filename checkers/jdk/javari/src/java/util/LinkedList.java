package java.util;
import checkers.javari.quals.*;

public class LinkedList<E> extends java.util.AbstractSequentialList<E> implements java.util.List<E>, java.util.Deque<E>, java.lang.Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public LinkedList() { throw new RuntimeException(("skeleton method")); }
  public LinkedList(@PolyRead java.util.Collection<? extends E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public E getFirst() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E getLast() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E removeFirst() { throw new RuntimeException(("skeleton method")); }
  public E removeLast() { throw new RuntimeException(("skeleton method")); }
  public void addFirst(E a1) { throw new RuntimeException(("skeleton method")); }
  public void addLast(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public E get(int a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E set(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public void add(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public E remove(int a1) { throw new RuntimeException(("skeleton method")); }
  public int indexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public int lastIndexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E peek() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E element() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E poll() { throw new RuntimeException(("skeleton method")); }
  public E remove() { throw new RuntimeException(("skeleton method")); }
  public boolean offer(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean offerFirst(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean offerLast(E a1) { throw new RuntimeException(("skeleton method")); }
  public E peekFirst() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E peekLast() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E pollFirst() { throw new RuntimeException(("skeleton method")); }
  public E pollLast() { throw new RuntimeException(("skeleton method")); }
  public void push(E a1) { throw new RuntimeException(("skeleton method")); }
  public E pop() { throw new RuntimeException(("skeleton method")); }
  public boolean removeFirstOccurrence(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean removeLastOccurrence(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.ListIterator<E> listIterator(int a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Iterator<E> descendingIterator() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public java.lang.Object[] toArray() { throw new RuntimeException(("skeleton method")); }
  public <T> T[] toArray(T[] a1) { throw new RuntimeException(("skeleton method")); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
