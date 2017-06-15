package java.util;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Queue<E> extends Collection<E> {
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract @Nullable E poll();
  public abstract E element();
  public abstract @Nullable E peek();
  @EnsuresNonNullIf(expression={"poll()", "peek()"}, result=false)
  @Pure public abstract boolean isEmpty();
}
