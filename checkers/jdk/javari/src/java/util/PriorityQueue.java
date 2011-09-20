package java.util;
import checkers.javari.quals.*;

public class PriorityQueue<E> extends AbstractQueue<E> implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public PriorityQueue() { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(int a1) { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(int a1, Comparator<? super E> a2) { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(@PolyRead PriorityQueue<E> this, @PolyRead Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(@PolyRead PriorityQueue<E> this, @PolyRead PriorityQueue<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(@PolyRead PriorityQueue<E> this, @PolyRead SortedSet<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean offer(E a1) { throw new RuntimeException(("skeleton method")); }
  public E peek(@ReadOnly PriorityQueue<E> this) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly PriorityQueue<E> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public Object[] toArray() { throw new RuntimeException(("skeleton method")); }
  public <T> T[] toArray(T[] a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Iterator<E> iterator(@PolyRead PriorityQueue<E> this){ throw new RuntimeException(("skeleton method")); }
  public int size(@ReadOnly PriorityQueue<E> this) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public E poll() { throw new RuntimeException(("skeleton method")); }
  public Comparator<? super E> comparator(@ReadOnly PriorityQueue<E> this){ throw new RuntimeException(("skeleton method")); }
}
