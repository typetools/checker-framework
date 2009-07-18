package java.util;
import checkers.javari.quals.*;

public interface NavigableSet<E> extends java.util.SortedSet<E> {
  public abstract E lower(E a1) @ReadOnly;
  public abstract E floor(E a1) @ReadOnly;
  public abstract E ceiling(E a1) @ReadOnly;
  public abstract E higher(E a1) @ReadOnly;
  public abstract E pollFirst();
  public abstract E pollLast();
  public abstract @PolyRead java.util.Iterator<E> iterator() @PolyRead;
  public abstract @PolyRead java.util.NavigableSet<E> descendingSet() @PolyRead;
  public abstract @PolyRead java.util.Iterator<E> descendingIterator() @PolyRead;
  public abstract @PolyRead java.util.NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) @PolyRead;
  public abstract @PolyRead java.util.NavigableSet<E> headSet(E a1, boolean a2) @PolyRead;
  public abstract @PolyRead java.util.NavigableSet<E> tailSet(E a1, boolean a2) @PolyRead;
  public abstract @PolyRead java.util.SortedSet<E> subSet(E a1, E a2) @PolyRead;
  public abstract @PolyRead java.util.SortedSet<E> headSet(E a1) @PolyRead;
  public abstract @PolyRead java.util.SortedSet<E> tailSet(E a1) @PolyRead;
}
