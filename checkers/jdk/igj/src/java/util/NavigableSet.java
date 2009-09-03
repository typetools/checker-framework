package java.util;
import checkers.igj.quals.*;

@I
public interface NavigableSet<E> extends @I java.util.SortedSet<E> {
  public abstract E lower(E a1) @ReadOnly;
  public abstract E floor(E a1) @ReadOnly;
  public abstract E ceiling(E a1) @ReadOnly;
  public abstract E higher(E a1) @ReadOnly;
  public abstract E pollFirst() @Mutable;
  public abstract E pollLast() @Mutable;
  public abstract @I java.util.Iterator<E> iterator() @ReadOnly;
  public abstract @I java.util.NavigableSet<E> descendingSet() @ReadOnly;
  public abstract @I java.util.Iterator<E> descendingIterator() @ReadOnly;
  public abstract @I java.util.NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) @ReadOnly;
  public abstract @I java.util.NavigableSet<E> headSet(E a1, boolean a2) @ReadOnly;
  public abstract @I java.util.NavigableSet<E> tailSet(E a1, boolean a2) @ReadOnly;
  public abstract @I java.util.SortedSet<E> subSet(E a1, E a2) @ReadOnly;
  public abstract @I java.util.SortedSet<E> headSet(E a1) @ReadOnly;
  public abstract @I java.util.SortedSet<E> tailSet(E a1) @ReadOnly;
}
