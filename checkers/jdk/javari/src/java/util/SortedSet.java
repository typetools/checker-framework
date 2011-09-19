package java.util;
import checkers.javari.quals.*;

public interface SortedSet<E> extends Set<E> {
  public abstract Comparator<? super E> comparator(@ReadOnly SortedSet this);
  public abstract @PolyRead SortedSet<E> subSet(@PolyRead SortedSet this, E a1, E a2);
  public abstract @PolyRead SortedSet<E> headSet(@PolyRead SortedSet this, E a1);
  public abstract @PolyRead SortedSet<E> tailSet(@PolyRead SortedSet this, E a1);
  public abstract E first(@ReadOnly SortedSet this);
  public abstract E last(@ReadOnly SortedSet this);
}
