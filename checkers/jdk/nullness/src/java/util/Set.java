package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface Set<E extends @Nullable Object> extends Collection<E> {
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract boolean contains(@Nullable Object a1);
  public abstract Iterator<E> iterator();
  public abstract Object [] toArray();
  public abstract <T> @Nullable T @PolyNull [] toArray(T @PolyNull [] a1);
  public abstract boolean add(E a1);
  public abstract boolean remove(@Nullable Object a1);
  public abstract boolean containsAll(Collection<?> a1);
  public abstract boolean addAll(Collection<? extends E> a1);
  public abstract boolean retainAll(Collection<?> a1);
  public abstract boolean removeAll(Collection<?> a1);
  public abstract void clear();
  public abstract boolean equals(@Nullable Object a1);
  public abstract int hashCode();
}
