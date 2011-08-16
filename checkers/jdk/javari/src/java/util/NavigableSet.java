package java.util;
import checkers.javari.quals.*;

public interface NavigableSet<E> extends SortedSet<E> {
  public abstract E lower(E a1) @ReadOnly;
  public abstract E floor(E a1) @ReadOnly;
  public abstract E ceiling(E a1) @ReadOnly;
  public abstract E higher(E a1) @ReadOnly;
  public abstract E pollFirst();
  public abstract E pollLast();
  public abstract @PolyRead Iterator<E> iterator() @PolyRead;
  public abstract @PolyRead NavigableSet<E> descendingSet() @PolyRead;
  public abstract @PolyRead Iterator<E> descendingIterator() @PolyRead;
  public abstract @PolyRead NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4) @PolyRead;
  public abstract @PolyRead NavigableSet<E> headSet(E a1, boolean a2) @PolyRead;
  public abstract @PolyRead NavigableSet<E> tailSet(E a1, boolean a2) @PolyRead;
  public abstract @PolyRead SortedSet<E> subSet(E a1, E a2) @PolyRead;
  public abstract @PolyRead SortedSet<E> headSet(E a1) @PolyRead;
  public abstract @PolyRead SortedSet<E> tailSet(E a1) @PolyRead;
}
