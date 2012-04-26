package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// This class permits null elements
public class LinkedList<E extends @Nullable Object> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;

  public LinkedList() { throw new RuntimeException("skeleton method"); }
  public LinkedList(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public E getFirst() { throw new RuntimeException("skeleton method"); }
  public E getLast() { throw new RuntimeException("skeleton method"); }
  public E removeFirst() { throw new RuntimeException("skeleton method"); }
  public E removeLast() { throw new RuntimeException("skeleton method"); }
  public void addFirst(E a1) { throw new RuntimeException("skeleton method"); }
  public void addLast(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public @Pure E get(int a1) { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
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
  public boolean removeFirstOccurrence(Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(Object a1) { throw new RuntimeException("skeleton method"); }
  public ListIterator<E> listIterator(int a1) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public @Nullable Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T extends @Nullable Object> @Nullable T @PolyNull [] toArray(T @PolyNull [] a1) { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }

  @AssertNonNullIfFalse({"peek()", "peekFirst()", "peekLast()", "poll()", "pollFirst()", "pollLast()"})
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
}
