package java.util;
import checkers.igj.quals.*;

@I
public interface SortedSet<E> extends @I Set<E> {
  public abstract @ReadOnly Comparator<? super E> comparator() @ReadOnly;
  public abstract @I SortedSet<E> subSet(E a1, E a2) @ReadOnly;
  public abstract @I SortedSet<E> headSet(E a1) @ReadOnly;
  public abstract @I SortedSet<E> tailSet(E a1) @ReadOnly;
  public abstract E first() @ReadOnly;
  public abstract E last() @ReadOnly;
}
