package java.util;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

// Subclasses of this interface/class may opt to prohibit null elements
public interface SortedSet<E> extends Set<E> {
  @SideEffectFree public abstract Comparator<? super E> comparator();
  @SideEffectFree public abstract SortedSet<E> subSet(E a1, E a2);
  @SideEffectFree public abstract SortedSet<E> headSet(E a1);
  @SideEffectFree public abstract SortedSet<E> tailSet(E a1);
  @SideEffectFree public abstract E first();
  @SideEffectFree public abstract E last();
}
