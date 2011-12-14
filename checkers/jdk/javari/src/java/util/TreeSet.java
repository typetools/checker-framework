package java.util;
import checkers.javari.quals.*;

public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeSet() { throw new RuntimeException(("skeleton method")); }
  public TreeSet(Comparator<? super E> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeSet(@PolyRead TreeSet<E> this, @PolyRead Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeSet(@PolyRead TreeSet<E> this, @PolyRead SortedSet<E> a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Iterator<E> iterator(@PolyRead TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Iterator<E> descendingIterator(@PolyRead TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> descendingSet(@PolyRead TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public int size(@ReadOnly TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public boolean isEmpty(@ReadOnly TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly TreeSet<E> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(@ReadOnly Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> subSet(@PolyRead TreeSet<E> this, E a1, boolean a2, E a3, boolean a4) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> headSet(@PolyRead TreeSet<E> this, E a1, boolean a2) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> tailSet(@PolyRead TreeSet<E> this, E a1, boolean a2) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedSet<E> subSet(@PolyRead TreeSet<E> this, E a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedSet<E> headSet(@PolyRead TreeSet<E> this, E a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedSet<E> tailSet(@PolyRead TreeSet<E> this, E a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Comparator<? super E> comparator(@ReadOnly TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public E first(@ReadOnly TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public E last(@ReadOnly TreeSet<E> this) { throw new RuntimeException(("skeleton method")); }
  public E lower(@ReadOnly TreeSet<E> this, E a1) { throw new RuntimeException(("skeleton method")); }
  public E floor(@ReadOnly TreeSet<E> this, E a1) { throw new RuntimeException(("skeleton method")); }
  public E ceiling(@ReadOnly TreeSet<E> this, E a1) { throw new RuntimeException(("skeleton method")); }
  public E higher(@ReadOnly TreeSet<E> this, E a1) { throw new RuntimeException(("skeleton method")); }
  public E pollFirst() { throw new RuntimeException(("skeleton method")); }
  public E pollLast() { throw new RuntimeException(("skeleton method")); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
