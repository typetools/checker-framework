package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface NavigableSet<E extends @Nullable Object> extends SortedSet<E> {
  public abstract @Nullable E lower(E a1);
  public abstract @Nullable E floor(E a1);
  public abstract @Nullable E ceiling(E a1);
  public abstract @Nullable E higher(E a1);
  public abstract @Nullable E pollFirst();
  public abstract @Nullable E pollLast();
  public abstract Iterator<E> iterator();
  public abstract NavigableSet<E> descendingSet();
  public abstract Iterator<E> descendingIterator();
  public abstract NavigableSet<E> subSet(E a1, boolean a2, E a3, boolean a4);
  public abstract NavigableSet<E> headSet(E a1, boolean a2);
  public abstract NavigableSet<E> tailSet(E a1, boolean a2);
  public abstract SortedSet<E> subSet(E a1, E a2);
  public abstract SortedSet<E> headSet(E a1);
  public abstract SortedSet<E> tailSet(E a1);

  @AssertNonNullIfFalse({"pollFirst()", "pollLast()"})
  public abstract boolean isEmpty();

}
