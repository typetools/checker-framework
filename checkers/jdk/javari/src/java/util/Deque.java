package java.util;
import checkers.javari.quals.*;

public interface Deque<E> extends Queue<E> {
  public abstract void addFirst(E a1);
  public abstract void addLast(E a1);
  public abstract boolean offerFirst(E a1);
  public abstract boolean offerLast(E a1);
  public abstract E removeFirst();
  public abstract E removeLast();
  public abstract E pollFirst();
  public abstract E pollLast();
  public abstract E getFirst(@ReadOnly Deque<E> this);
  public abstract E getLast(@ReadOnly Deque<E> this) ;
  public abstract E peekFirst(@ReadOnly Deque<E> this);
  public abstract E peekLast(@ReadOnly Deque<E> this);
  public abstract boolean removeFirstOccurrence(@ReadOnly Object a1);
  public abstract boolean removeLastOccurrence(@ReadOnly Object a1);
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract E poll();
  public abstract E element();
  public abstract E peek(@ReadOnly Deque<E> this);
  public abstract void push(E a1);
  public abstract E pop();
  public abstract boolean remove(@ReadOnly Object a1);
  public abstract boolean contains(@ReadOnly Deque<E> this, @ReadOnly Object a1);
  public abstract int size(@ReadOnly Deque<E> this);
  public abstract @PolyRead Iterator<E> iterator(@PolyRead Deque<E> this);
  public abstract @PolyRead Iterator<E> descendingIterator(@PolyRead Deque<E> this);
}
