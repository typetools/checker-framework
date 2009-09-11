package java.util;
import checkers.javari.quals.*;

public interface SortedSet<E> extends java.util.Set<E> {
  public abstract java.util.Comparator<? super E> comparator() @ReadOnly;
  public abstract @PolyRead java.util.SortedSet<E> subSet(E a1, E a2) @PolyRead;
  public abstract @PolyRead java.util.SortedSet<E> headSet(E a1) @PolyRead;
  public abstract @PolyRead java.util.SortedSet<E> tailSet(E a1) @PolyRead;
  public abstract E first() @ReadOnly;
  public abstract E last() @ReadOnly;
}
