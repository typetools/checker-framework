package java.util;
import checkers.javari.quals.*;

public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public LinkedList() { throw new RuntimeException(("skeleton method")); }
  public LinkedList(@PolyRead LinkedList<E> this, @PolyRead Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public E getFirst(@ReadOnly LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public E getLast(@ReadOnly LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public E removeFirst() { throw new RuntimeException(("skeleton method")); }
  public E removeLast() { throw new RuntimeException(("skeleton method")); }
  public void addFirst(E a1) { throw new RuntimeException(("skeleton method")); }
  public void addLast(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly LinkedList<E> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public int size(@ReadOnly LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(@ReadOnly Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(int a1, @ReadOnly Collection<? extends E> a2) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public E get(@ReadOnly LinkedList<E> this, int a1) { throw new RuntimeException(("skeleton method")); }
  public E set(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public void add(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public E remove(int a1) { throw new RuntimeException(("skeleton method")); }
  public int indexOf(@ReadOnly LinkedList<E> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public int lastIndexOf(@ReadOnly LinkedList<E> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public E peek(@ReadOnly LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public E element(@ReadOnly LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public E poll() { throw new RuntimeException(("skeleton method")); }
  public E remove() { throw new RuntimeException(("skeleton method")); }
  public boolean offer(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean offerFirst(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean offerLast(E a1) { throw new RuntimeException(("skeleton method")); }
  public E peekFirst(@ReadOnly LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public E peekLast(@ReadOnly LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public E pollFirst() { throw new RuntimeException(("skeleton method")); }
  public E pollLast() { throw new RuntimeException(("skeleton method")); }
  public void push(E a1) { throw new RuntimeException(("skeleton method")); }
  public E pop() { throw new RuntimeException(("skeleton method")); }
  public boolean removeFirstOccurrence(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean removeLastOccurrence(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead ListIterator<E> listIterator(@PolyRead LinkedList<E> this, int a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Iterator<E> descendingIterator(@PolyRead LinkedList<E> this) { throw new RuntimeException(("skeleton method")); }
  public Object[] toArray() { throw new RuntimeException(("skeleton method")); }
  public <T> T[] toArray(T[] a1) { throw new RuntimeException(("skeleton method")); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
