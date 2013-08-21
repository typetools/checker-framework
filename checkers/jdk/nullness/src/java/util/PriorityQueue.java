package java.util;
import dataflow.quals.Pure;
import checkers.nullness.quals.EnsuresNonNullIf;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;

// doesn't permit null element
public class PriorityQueue<E extends @NonNull Object> extends AbstractQueue<E> implements java.io.Serializable {
  private static final long serialVersionUID = 0;
  public PriorityQueue() { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1, Comparator<? super E> a2) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(PriorityQueue<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(SortedSet<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E peek() { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> @Nullable T @PolyNull [] toArray(T @PolyNull [] a1) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  @Pure public int size() { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public @Nullable E poll() { throw new RuntimeException("skeleton method"); }
  public Comparator<? super E> comparator() { throw new RuntimeException("skeleton method"); }

  @EnsuresNonNullIf(expression={"poll()", "peek()"}, result=false)
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
}
