package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {
  protected AbstractQueue() {}
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public E remove() { throw new RuntimeException("skeleton method"); }
  public E element() { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  //@EnsuresNonNullIf(expression={"poll()", "peek()"}, result=false)
  //@Pure public abstract boolean isEmpty();
}
