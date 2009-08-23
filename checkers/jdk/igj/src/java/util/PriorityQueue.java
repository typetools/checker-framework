package java.util;
import checkers.igj.quals.*;

@I
public class PriorityQueue<E> extends @I java.util.AbstractQueue<E> implements @I java.io.Serializable {
  public PriorityQueue() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1, @ReadOnly java.util.Comparator<? super E> a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(@ReadOnly java.util.Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(@ReadOnly java.util.PriorityQueue<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(@ReadOnly java.util.SortedSet<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E peek() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean remove(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public <T> T[] toArray(T[] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public E poll() @Mutable { throw new RuntimeException("skeleton method"); }
  public @ReadOnly java.util.Comparator<? super E> comparator() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
