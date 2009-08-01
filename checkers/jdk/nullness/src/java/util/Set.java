package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface Set<E extends @NonNull Object> extends java.util.Collection<E> {
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract boolean contains(@Nullable java.lang.Object a1);
  public abstract java.util.Iterator<E> iterator();
  public abstract @Nullable java.lang.Object [] toArray();
  public abstract <T> @Nullable T [] toArray(T[] a1);
  public abstract boolean add(E a1);
  public abstract boolean remove(@Nullable java.lang.Object a1);
  public abstract boolean containsAll(java.util.Collection<?> a1);
  public abstract boolean addAll(java.util.Collection<? extends E> a1);
  public abstract boolean retainAll(java.util.Collection<?> a1);
  public abstract boolean removeAll(java.util.Collection<?> a1);
  public abstract void clear();
  public abstract boolean equals(@Nullable java.lang.Object a1);
  public abstract int hashCode();
}
