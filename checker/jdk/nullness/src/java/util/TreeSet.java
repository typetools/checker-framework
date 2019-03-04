package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

// Subclasses of this interface/class may opt to prohibit null elements
public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public TreeSet() { throw new RuntimeException("skeleton method"); }
  public TreeSet(Comparator<? super E> a1) { throw new RuntimeException("skeleton method"); }
  public TreeSet(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public TreeSet(SortedSet<E> a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public NavigableSet<E> descendingSet() { throw new RuntimeException("skeleton method"); }
  @Pure public int size() { throw new RuntimeException("skeleton method"); }
  @EnsuresNonNullIf(expression={"pollFirst()", "pollLast()"}, result=false)
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contains(Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public NavigableSet<E> headSet(E a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public NavigableSet<E> tailSet(E a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public SortedSet<E> subSet(E a1, E a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public SortedSet<E> headSet(E a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public SortedSet<E> tailSet(E a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Comparator<? super E> comparator() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public E first() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public E last() { throw new RuntimeException("skeleton method"); }
  public @Nullable E lower(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E floor(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E ceiling(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E higher(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E pollFirst() { throw new RuntimeException("skeleton method"); }
  public @Nullable E pollLast() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Object clone() { throw new RuntimeException("skeleton method"); }
}
