package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface Queue<E extends @Nullable Object> extends Collection<E> {
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract @Nullable E poll();
  public abstract E element();
  public abstract @Nullable E peek();
  @AssertNonNullIfFalse({"poll()", "peek()"})
  public abstract boolean isEmpty();
}
