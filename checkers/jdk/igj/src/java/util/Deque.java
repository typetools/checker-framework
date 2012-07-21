package java.util;
import checkers.igj.quals.*;

@I
public interface Deque<E> extends @I Queue<E> {
  public abstract void addFirst(@Mutable Deque<E> this, E a1);
  public abstract void addLast(@Mutable Deque<E> this, E a1);
  public abstract boolean offerFirst(@Mutable Deque<E> this, E a1);
  public abstract boolean offerLast(@Mutable Deque<E> this, E a1);
  public abstract E removeFirst(@Mutable Deque<E> this);
  public abstract E removeLast(@Mutable Deque<E> this);
  public abstract E pollFirst(@Mutable Deque<E> this);
  public abstract E pollLast(@Mutable Deque<E> this);
  public abstract E getFirst(@ReadOnly Deque<E> this);
  public abstract E getLast(@ReadOnly Deque<E> this);
  public abstract E peekFirst(@ReadOnly Deque<E> this);
  public abstract E peekLast(@ReadOnly Deque<E> this);
  public abstract boolean removeFirstOccurrence(@Mutable Deque<E> this, @ReadOnly Object a1);
  public abstract boolean removeLastOccurrence(@Mutable Deque<E> this, @ReadOnly Object a1);
  public abstract boolean add(@Mutable Deque<E> this, E a1);
  public abstract boolean offer(@Mutable Deque<E> this, E a1);
  public abstract E remove(@Mutable Deque<E> this);
  public abstract E poll(@Mutable Deque<E> this);
  public abstract E element(@ReadOnly Deque<E> this);
  public abstract E peek(@ReadOnly Deque<E> this);
  public abstract void push(@Mutable Deque<E> this, E a1);
  public abstract E pop(@Mutable Deque<E> this);
  public abstract boolean remove(@Mutable Deque<E> this, @ReadOnly Object a1);
  public abstract boolean contains(@ReadOnly Deque<E> this, @ReadOnly Object a1);
  public abstract int size(@ReadOnly Deque<E> this);
  public abstract @I Iterator<E> iterator(@ReadOnly Deque<E> this);
  public abstract @I Iterator<E> descendingIterator(@ReadOnly Deque<E> this);
}
