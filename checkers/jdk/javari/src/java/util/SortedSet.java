package java.util;
import checkers.javari.quals.*;

public interface SortedSet<E> extends Set<E> {
  public abstract Comparator<? super E> comparator(@ReadOnly SortedSet<E> this);
  public abstract @PolyRead SortedSet<E> subSet(@PolyRead SortedSet<E> this, E a1, E a2);
  public abstract @PolyRead SortedSet<E> headSet(@PolyRead SortedSet<E> this, E a1);
  public abstract @PolyRead SortedSet<E> tailSet(@PolyRead SortedSet<E> this, E a1);
  public abstract E first(@ReadOnly SortedSet<E> this);
  public abstract E last(@ReadOnly SortedSet<E> this);
}
