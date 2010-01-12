package java.util;
import checkers.igj.quals.*;

@I
public class TreeSet<E> extends @I java.util.AbstractSet<E> implements @I java.util.NavigableSet<E>, @I java.lang.Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeSet() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public TreeSet(@ReadOnly java.util.Comparator<? super E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public TreeSet(@ReadOnly java.util.Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public TreeSet(@ReadOnly java.util.SortedSet<E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> iterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Iterator<E> descendingIterator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.NavigableSet<E> descendingSet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean remove(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I java.util.NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.NavigableSet<E> headSet(E a1, boolean a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.NavigableSet<E> tailSet(E a1, boolean a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.SortedSet<E> subSet(E a1, E a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.SortedSet<E> headSet(E a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.SortedSet<E> tailSet(E a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @ReadOnly java.util.Comparator<? super E> comparator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E first() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E last() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E lower(E a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E floor(E a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E ceiling(E a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E higher(E a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E pollFirst() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public E pollLast() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
