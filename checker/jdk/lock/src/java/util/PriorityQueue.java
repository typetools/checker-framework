package java.util;


import org.checkerframework.checker.lock.qual.*;


// doesn't permit null element
public class PriorityQueue<E extends Object> extends AbstractQueue<E> implements java.io.Serializable {
  private static final long serialVersionUID = 0;
  public PriorityQueue() { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(int a1, Comparator<? super E> a2) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(PriorityQueue<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public PriorityQueue(SortedSet<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean offer(E a1) { throw new RuntimeException("skeleton method"); }
  public E peek() { throw new RuntimeException("skeleton method"); }
  public boolean remove(Object a1) { throw new RuntimeException("skeleton method"); }
   public boolean contains(@GuardSatisfied PriorityQueue<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied PriorityQueue<E> this) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public E poll() { throw new RuntimeException("skeleton method"); }
   public Comparator<? super E> comparator(@GuardSatisfied PriorityQueue<E> this) { throw new RuntimeException("skeleton method"); }


   public boolean isEmpty(@GuardSatisfied PriorityQueue<E> this) { throw new RuntimeException("skeleton method"); }
}
