package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public class TreeSet<E extends @NonNull Object> extends java.util.AbstractSet<E> implements java.util.NavigableSet<E>, java.lang.Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public TreeSet() { throw new RuntimeException("skeleton method"); }
  public TreeSet(java.util.Comparator<? super E> a1) { throw new RuntimeException("skeleton method"); }
  public TreeSet(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public TreeSet(java.util.SortedSet<E> a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public java.util.Iterator<E> descendingIterator() { throw new RuntimeException("skeleton method"); }
  public java.util.NavigableSet<E> descendingSet() { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public java.util.NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) { throw new RuntimeException("skeleton method"); }
  public java.util.NavigableSet<E> headSet(E a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public java.util.NavigableSet<E> tailSet(E a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public java.util.SortedSet<E> subSet(E a1, E a2) { throw new RuntimeException("skeleton method"); }
  public java.util.SortedSet<E> headSet(E a1) { throw new RuntimeException("skeleton method"); }
  public java.util.SortedSet<E> tailSet(E a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Comparator<? super E> comparator() { throw new RuntimeException("skeleton method"); }
  public @Nullable E first() { throw new RuntimeException("skeleton method"); }
  public @Nullable E last() { throw new RuntimeException("skeleton method"); }
  public @Nullable E lower(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E floor(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E ceiling(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E higher(E a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable E pollFirst() { throw new RuntimeException("skeleton method"); }
  public @Nullable E pollLast() { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
