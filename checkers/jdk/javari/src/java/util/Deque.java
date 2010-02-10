package java.util;
import checkers.javari.quals.*;

public interface Deque<E> extends java.util.Queue<E> {
  public abstract void addFirst(E a1);
  public abstract void addLast(E a1);
  public abstract boolean offerFirst(E a1);
  public abstract boolean offerLast(E a1);
  public abstract E removeFirst();
  public abstract E removeLast();
  public abstract E pollFirst();
  public abstract E pollLast();
  public abstract E getFirst() @ReadOnly;
  public abstract E getLast() @ReadOnly ;
  public abstract E peekFirst() @ReadOnly;
  public abstract E peekLast() @ReadOnly;
  public abstract boolean removeFirstOccurrence(@ReadOnly java.lang.Object a1);
  public abstract boolean removeLastOccurrence(@ReadOnly java.lang.Object a1);
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract E poll();
  public abstract E element();
  public abstract E peek() @ReadOnly;
  public abstract void push(E a1);
  public abstract E pop();
  public abstract boolean remove(@ReadOnly java.lang.Object a1);
  public abstract boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly;
  public abstract int size() @ReadOnly;
  public abstract @PolyRead java.util.Iterator<E> iterator() @PolyRead;
  public abstract @PolyRead java.util.Iterator<E> descendingIterator() @PolyRead;
}
