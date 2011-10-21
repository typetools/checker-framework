package java.util;
import checkers.igj.quals.*;

@I
public interface SortedSet<E> extends @I Set<E> {
  public abstract @ReadOnly Comparator<? super E> comparator(@ReadOnly SortedSet<E> this);
  public abstract @I SortedSet<E> subSet(@ReadOnly SortedSet<E> this, E a1, E a2);
  public abstract @I SortedSet<E> headSet(@ReadOnly SortedSet<E> this, E a1);
  public abstract @I SortedSet<E> tailSet(@ReadOnly SortedSet<E> this, E a1);
  public abstract E first(@ReadOnly SortedSet<E> this);
  public abstract E last(@ReadOnly SortedSet<E> this);
}
