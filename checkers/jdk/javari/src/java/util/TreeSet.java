package java.util;
import checkers.javari.quals.*;

public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeSet() { throw new RuntimeException(("skeleton method")); }
  public TreeSet(Comparator<? super E> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeSet(@PolyRead Collection<? extends E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public TreeSet(@PolyRead SortedSet<E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Iterator<E> iterator() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Iterator<E> descendingIterator() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> descendingSet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(@ReadOnly Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> headSet(E a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<E> tailSet(E a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedSet<E> subSet(E a1, E a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedSet<E> headSet(E a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedSet<E> tailSet(E a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Comparator<? super E> comparator() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E first() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E last() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E lower(E a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E floor(E a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E ceiling(E a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E higher(E a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public E pollFirst() { throw new RuntimeException(("skeleton method")); }
  public E pollLast() { throw new RuntimeException(("skeleton method")); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
