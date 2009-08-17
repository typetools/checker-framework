package java.util;
import checkers.igj.quals.*;

@I
public class LinkedList<E> extends @I java.util.AbstractSequentialList<E> implements @I java.util.List<E>, @I java.util.Deque<E>, @I java.lang.Cloneable, @I java.io.Serializable {
  public LinkedList() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public LinkedList(@ReadOnly java.util.Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public E getFirst() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E getLast() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E removeFirst() @Mutable { throw new RuntimeException("skeleton method"); }
  public E removeLast() @Mutable { throw new RuntimeException("skeleton method"); }
  public void addFirst(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void addLast(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean remove(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public E get(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E set(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public int indexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E peek() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E element() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E poll() @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offerFirst(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offerLast(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E peekFirst() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E peekLast() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E pollFirst() @Mutable { throw new RuntimeException("skeleton method"); }
  public E pollLast() @Mutable { throw new RuntimeException("skeleton method"); }
  public void push(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E pop() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean removeFirstOccurrence(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I java.util.ListIterator<E> listIterator(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> descendingIterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public <T> T[] toArray(T[] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
}
