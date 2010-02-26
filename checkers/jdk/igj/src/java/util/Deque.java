package java.util;
import checkers.igj.quals.*;

@I
public interface Deque<E> extends @I Queue<E> {
  public abstract void addFirst(E a1) @Mutable;
  public abstract void addLast(E a1) @Mutable;
  public abstract boolean offerFirst(E a1) @Mutable;
  public abstract boolean offerLast(E a1) @Mutable;
  public abstract E removeFirst() @Mutable;
  public abstract E removeLast() @Mutable;
  public abstract E pollFirst() @Mutable;
  public abstract E pollLast() @Mutable;
  public abstract E getFirst() @ReadOnly;
  public abstract E getLast() @ReadOnly;
  public abstract E peekFirst() @ReadOnly;
  public abstract E peekLast() @ReadOnly;
  public abstract boolean removeFirstOccurrence(@ReadOnly Object a1) @Mutable;
  public abstract boolean removeLastOccurrence(@ReadOnly Object a1) @Mutable;
  public abstract boolean add(E a1) @Mutable;
  public abstract boolean offer(E a1) @Mutable;
  public abstract E remove() @Mutable;
  public abstract E poll() @Mutable;
  public abstract E element() @ReadOnly;
  public abstract E peek() @ReadOnly;
  public abstract void push(E a1) @Mutable;
  public abstract E pop() @Mutable;
  public abstract boolean remove(@ReadOnly Object a1) @Mutable;
  public abstract boolean contains(@ReadOnly Object a1) @ReadOnly;
  public abstract int size() @ReadOnly;
  public abstract @I Iterator<E> iterator() @ReadOnly;
  public abstract @I Iterator<E> descendingIterator() @ReadOnly;
}
