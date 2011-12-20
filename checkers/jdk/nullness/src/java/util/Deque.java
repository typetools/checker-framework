package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit
// null elements
public interface Deque<E extends @Nullable Object> extends Queue<E> {
  public abstract void addFirst(E a1);
  public abstract void addLast(E a1);
  public abstract boolean offerFirst(E a1);
  public abstract boolean offerLast(E a1);
  public abstract E removeFirst();
  public abstract E removeLast();
  public abstract E getFirst();
  public abstract E getLast();
  public abstract @Nullable E peekFirst();
  public abstract @Nullable E peekLast();
  public abstract boolean removeFirstOccurrence(Object a1);
  public abstract boolean removeLastOccurrence(Object a1);
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract @Nullable E poll();
  public abstract @Nullable E pollFirst();
  public abstract @Nullable E pollLast();
  public abstract E element();
  public abstract @Nullable E peek();
  public abstract void push(E a1);
  public abstract E pop();
  public abstract boolean remove(@Nullable Object a1);
  public abstract boolean contains(@Nullable Object a1);
  public abstract int size();
  public abstract Iterator<E> iterator();
  public abstract Iterator<E> descendingIterator();
  @AssertNonNullIfFalse({"peek()", "peekFirst()", "peekLast()", "poll()", "pollFirst()", "pollLast()"})
  public abstract boolean isEmpty();
}
