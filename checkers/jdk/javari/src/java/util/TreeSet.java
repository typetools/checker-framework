package java.util;
import checkers.javari.quals.*;

public class TreeSet<E> extends java.util.AbstractSet<E> implements java.util.NavigableSet<E>, java.lang.Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeSet() { throw new RuntimeException(("skeleton method")); }
  public TreeSet(java.util.Comparator<? super E> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeSet(@PolyRead java.util.Collection<? extends E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public TreeSet(@PolyRead java.util.SortedSet<E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Iterator<E> iterator() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Iterator<E> descendingIterator() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableSet<E> descendingSet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableSet<E> headSet(E a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableSet<E> tailSet(E a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.SortedSet<E> subSet(E a1, E a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.SortedSet<E> headSet(E a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.SortedSet<E> tailSet(E a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Comparator<? super E> comparator() @ReadOnly { throw new RuntimeException(("skeleton method")); }
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
