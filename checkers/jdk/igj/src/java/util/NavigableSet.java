package java.util;
import checkers.igj.quals.*;

@I
public interface NavigableSet<E> extends @I SortedSet<E> {
  public abstract E lower(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E floor(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E ceiling(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E higher(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E pollFirst(@Mutable NavigableSet<E> this);
  public abstract E pollLast(@Mutable NavigableSet<E> this);
  public abstract @I Iterator<E> iterator(@ReadOnly NavigableSet<E> this);
  public abstract @I NavigableSet<E> descendingSet(@ReadOnly NavigableSet<E> this);
  public abstract @I Iterator<E> descendingIterator(@ReadOnly NavigableSet<E> this);
  public abstract @I NavigableSet<E> subSet(@ReadOnly NavigableSet<E> this, E a1, boolean a2, E a3, boolean a4);
  public abstract @I NavigableSet<E> headSet(@ReadOnly NavigableSet<E> this, E a1, boolean a2);
  public abstract @I NavigableSet<E> tailSet(@ReadOnly NavigableSet<E> this, E a1, boolean a2);
  public abstract @I SortedSet<E> subSet(@ReadOnly NavigableSet<E> this, E a1, E a2);
  public abstract @I SortedSet<E> headSet(@ReadOnly NavigableSet<E> this, E a1);
  public abstract @I SortedSet<E> tailSet(@ReadOnly NavigableSet<E> this, E a1);
}
