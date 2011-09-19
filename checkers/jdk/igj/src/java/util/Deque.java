package java.util;
import checkers.igj.quals.*;

@I
public interface Deque<E> extends @I Queue<E> {
  public abstract void addFirst(@Mutable Deque this, E a1);
  public abstract void addLast(@Mutable Deque this, E a1);
  public abstract boolean offerFirst(@Mutable Deque this, E a1);
  public abstract boolean offerLast(@Mutable Deque this, E a1);
  public abstract E removeFirst(@Mutable Deque this);
  public abstract E removeLast(@Mutable Deque this);
  public abstract E pollFirst(@Mutable Deque this);
  public abstract E pollLast(@Mutable Deque this);
  public abstract E getFirst(@ReadOnly Deque this);
  public abstract E getLast(@ReadOnly Deque this);
  public abstract E peekFirst(@ReadOnly Deque this);
  public abstract E peekLast(@ReadOnly Deque this);
  public abstract boolean removeFirstOccurrence(@Mutable Deque this, @ReadOnly Object a1);
  public abstract boolean removeLastOccurrence(@Mutable Deque this, @ReadOnly Object a1);
  public abstract boolean add(@Mutable Deque this, E a1);
  public abstract boolean offer(@Mutable Deque this, E a1);
  public abstract E remove(@Mutable Deque this);
  public abstract E poll(@Mutable Deque this);
  public abstract E element(@ReadOnly Deque this);
  public abstract E peek(@ReadOnly Deque this);
  public abstract void push(@Mutable Deque this, E a1);
  public abstract E pop(@Mutable Deque this);
  public abstract boolean remove(@Mutable Deque this, @ReadOnly Object a1);
  public abstract boolean contains(@ReadOnly Deque this, @ReadOnly Object a1);
  public abstract int size(@ReadOnly Deque this);
  public abstract @I Iterator<E> iterator(@ReadOnly Deque this);
  public abstract @I Iterator<E> descendingIterator(@ReadOnly Deque this);
}
