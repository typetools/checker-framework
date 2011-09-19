package java.util;
import checkers.igj.quals.*;

@I
public interface SortedSet<E> extends @I Set<E> {
  public abstract @ReadOnly Comparator<? super E> comparator(@ReadOnly SortedSet this);
  public abstract @I SortedSet<E> subSet(@ReadOnly SortedSet this, E a1, E a2);
  public abstract @I SortedSet<E> headSet(@ReadOnly SortedSet this, E a1);
  public abstract @I SortedSet<E> tailSet(@ReadOnly SortedSet this, E a1);
  public abstract E first(@ReadOnly SortedSet this);
  public abstract E last(@ReadOnly SortedSet this);
}
