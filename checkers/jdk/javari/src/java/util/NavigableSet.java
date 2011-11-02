package java.util;
import checkers.javari.quals.*;

public interface NavigableSet<E> extends SortedSet<E> {
  public abstract E lower(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E floor(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E ceiling(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E higher(@ReadOnly NavigableSet<E> this, E a1);
  public abstract E pollFirst();
  public abstract E pollLast();
  public abstract @PolyRead Iterator<E> iterator(@PolyRead NavigableSet<E> this);
  public abstract @PolyRead NavigableSet<E> descendingSet(@PolyRead NavigableSet<E> this);
  public abstract @PolyRead Iterator<E> descendingIterator(@PolyRead NavigableSet<E> this);
  public abstract @PolyRead NavigableSet<E> subSet(@PolyRead NavigableSet<E> this, E a1, boolean a2, E a3, boolean a4);
  public abstract @PolyRead NavigableSet<E> headSet(@PolyRead NavigableSet<E> this, E a1, boolean a2);
  public abstract @PolyRead NavigableSet<E> tailSet(@PolyRead NavigableSet<E> this, E a1, boolean a2);
  public abstract @PolyRead SortedSet<E> subSet(@PolyRead NavigableSet<E> this, E a1, E a2);
  public abstract @PolyRead SortedSet<E> headSet(@PolyRead NavigableSet<E> this, E a1);
  public abstract @PolyRead SortedSet<E> tailSet(@PolyRead NavigableSet<E> this, E a1);
}
