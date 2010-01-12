package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface SortedSet<E extends @NonNull Object> extends java.util.Set<E> {
  public abstract java.util.Comparator<? super E> comparator();
  public abstract java.util.SortedSet<E> subSet(E a1, E a2);
  public abstract java.util.SortedSet<E> headSet(E a1);
  public abstract java.util.SortedSet<E> tailSet(E a1);
  public abstract E first();
  public abstract E last();
}
