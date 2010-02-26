package java.util;
import checkers.javari.quals.*;

public interface SortedSet<E> extends Set<E> {
  public abstract Comparator<? super E> comparator() @ReadOnly;
  public abstract @PolyRead SortedSet<E> subSet(E a1, E a2) @PolyRead;
  public abstract @PolyRead SortedSet<E> headSet(E a1) @PolyRead;
  public abstract @PolyRead SortedSet<E> tailSet(E a1) @PolyRead;
  public abstract E first() @ReadOnly;
  public abstract E last() @ReadOnly;
}
