package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

// Subclasses of this interface/class may opt to prohibit null elements
public interface NavigableSet<E> extends SortedSet<E> {
  public abstract @Nullable E lower(E a1);
  public abstract @Nullable E floor(E a1);
  public abstract @Nullable E ceiling(E a1);
  public abstract @Nullable E higher(E a1);
  public abstract @Nullable E pollFirst();
  public abstract @Nullable E pollLast();
  @SideEffectFree
  public abstract Iterator<E> iterator();
  public abstract NavigableSet<E> descendingSet();
  public abstract Iterator<E> descendingIterator();
  @SideEffectFree public abstract NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4);
  @SideEffectFree public abstract NavigableSet<E> headSet(E a1, boolean a2);
  @SideEffectFree public abstract NavigableSet<E> tailSet(E a1, boolean a2);
  @SideEffectFree public abstract SortedSet<E> subSet(E a1, E a2);
  @SideEffectFree public abstract SortedSet<E> headSet(E a1);
  @SideEffectFree public abstract SortedSet<E> tailSet(E a1);

  @EnsuresNonNullIf(expression={"pollFirst()", "pollLast()"}, result=false)
  @Pure public abstract boolean isEmpty();

}
