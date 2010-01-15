package java.util;
import checkers.igj.quals.*;

@I
public class ArrayDeque<E> extends @I java.util.AbstractCollection<E> implements @I java.util.Deque<E>, @I java.lang.Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public ArrayDeque() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public ArrayDeque(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public ArrayDeque(@ReadOnly java.util.Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void addFirst(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void addLast(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offerFirst(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offerLast(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E removeFirst() @Mutable { throw new RuntimeException("skeleton method"); }
  public E removeLast() @Mutable { throw new RuntimeException("skeleton method"); }
  public E pollFirst() @Mutable { throw new RuntimeException("skeleton method"); }
  public E pollLast() @Mutable { throw new RuntimeException("skeleton method"); }
  public E getFirst() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E getLast() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E peekFirst() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E peekLast() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean removeFirstOccurrence(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean removeLastOccurrence(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E remove() @Mutable { throw new RuntimeException("skeleton method"); }
  public E poll() @Mutable { throw new RuntimeException("skeleton method"); }
  public E element() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E peek() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void push(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E pop() @Mutable { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> descendingIterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean remove(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public java.lang.Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public <T> T[] toArray(T[] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") ArrayDeque<E> clone() { throw new RuntimeException("skeleton method"); }
}
