package java.util;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public class TreeSet<E extends Object> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public TreeSet() { throw new RuntimeException("skeleton method"); }
  public TreeSet(Comparator<? super E> a1) { throw new RuntimeException("skeleton method"); }
  public TreeSet(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public TreeSet(SortedSet<E> a1) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public NavigableSet<E> descendingSet() { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }

   public boolean isEmpty(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
   public boolean contains(@GuardSatisfied TreeSet<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public boolean containsAll(@GuardSatisfied TreeSet<E> this, @GuardSatisfied Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(@GuardSatisfied TreeSet<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@GuardSatisfied TreeSet<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@GuardSatisfied TreeSet<E> this, Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
   public NavigableSet<E> subSet(@GuardSatisfied TreeSet<E> this,@GuardSatisfied E a1, boolean a2, @GuardSatisfied E a3, boolean a4) { throw new RuntimeException("skeleton method"); }
   public NavigableSet<E> headSet(@GuardSatisfied TreeSet<E> this,@GuardSatisfied E a1, boolean a2) { throw new RuntimeException("skeleton method"); }
   public NavigableSet<E> tailSet(@GuardSatisfied TreeSet<E> this,@GuardSatisfied E a1, boolean a2) { throw new RuntimeException("skeleton method"); }
   public SortedSet<E> subSet(@GuardSatisfied TreeSet<E> this,@GuardSatisfied E a1, @GuardSatisfied E a2) { throw new RuntimeException("skeleton method"); }
   public SortedSet<E> headSet(@GuardSatisfied TreeSet<E> this,E a1) { throw new RuntimeException("skeleton method"); }
   public SortedSet<E> tailSet(@GuardSatisfied TreeSet<E> this,E a1) { throw new RuntimeException("skeleton method"); }
   public Comparator<? super E> comparator(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
   public E first(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
   public E last(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
  public E lower(E a1) { throw new RuntimeException("skeleton method"); }
  public E floor(E a1) { throw new RuntimeException("skeleton method"); }
  public E ceiling(E a1) { throw new RuntimeException("skeleton method"); }
  public E higher(E a1) { throw new RuntimeException("skeleton method"); }
  public E pollFirst(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
  public E pollLast(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied TreeSet<E> this) { throw new RuntimeException("skeleton method"); }
}
