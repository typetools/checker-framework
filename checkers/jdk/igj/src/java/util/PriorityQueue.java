package java.util;
import checkers.igj.quals.*;

@I
public class PriorityQueue<E> extends @I AbstractQueue<E> implements @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public PriorityQueue() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1, @ReadOnly Comparator<? super E> a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(@ReadOnly Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(@ReadOnly PriorityQueue<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(@ReadOnly SortedSet<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public E peek() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean remove(@ReadOnly Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public <T> T[] toArray(T[] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public E poll() @Mutable { throw new RuntimeException("skeleton method"); }
  public @ReadOnly Comparator<? super E> comparator() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
