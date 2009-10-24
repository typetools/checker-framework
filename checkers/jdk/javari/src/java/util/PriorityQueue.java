package java.util;
import checkers.javari.quals.*;

public class PriorityQueue<E> extends java.util.AbstractQueue<E> implements java.io.Serializable {
  public PriorityQueue() { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(int a1) { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(int a1, java.util.Comparator<? super E> a2) { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(@PolyRead java.util.Collection<? extends E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(@PolyRead java.util.PriorityQueue<? extends E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public PriorityQueue(@PolyRead java.util.SortedSet<? extends E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean offer(E a1) { throw new RuntimeException(("skeleton method")); }
  public E peek() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public java.lang.Object[] toArray() { throw new RuntimeException(("skeleton method")); }
  public <T> T[] toArray(T[] a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Iterator<E> iterator() @PolyRead{ throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public E poll() { throw new RuntimeException(("skeleton method")); }
  public java.util.Comparator<? super E> comparator() @ReadOnly{ throw new RuntimeException(("skeleton method")); }
}
