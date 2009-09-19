package java.util;
import checkers.igj.quals.*;

@I
public interface SortedSet<E> extends @I java.util.Set<E> {
  public abstract @ReadOnly java.util.Comparator<? super E> comparator() @ReadOnly;
  public abstract @I java.util.SortedSet<E> subSet(E a1, E a2) @ReadOnly;
  public abstract @I java.util.SortedSet<E> headSet(E a1) @ReadOnly;
  public abstract @I java.util.SortedSet<E> tailSet(E a1) @ReadOnly;
  public abstract E first() @ReadOnly;
  public abstract E last() @ReadOnly;
}
