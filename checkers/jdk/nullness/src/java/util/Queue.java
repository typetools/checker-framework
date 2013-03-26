package java.util;
import checkers.nullness.quals.EnsuresNonNullIf;
import checkers.nullness.quals.Nullable;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Queue<E extends @Nullable Object> extends Collection<E> {
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract @Nullable E poll();
  public abstract E element();
  public abstract @Nullable E peek();
  @EnsuresNonNullIf(expression={"poll()", "peek()"}, result=false)
  public abstract boolean isEmpty();
}
