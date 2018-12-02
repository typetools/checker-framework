package java.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Set<E> extends Collection<E> {
  @Pure public abstract int size();
  @Pure public abstract boolean isEmpty();
  @Pure public abstract boolean contains(@Nullable Object a1);
  @SideEffectFree public abstract Iterator<E> iterator();
  @SideEffectFree public abstract Object [] toArray();
  @SideEffectFree public abstract <T> @Nullable T @PolyNull [] toArray(T @PolyNull [] a1);
  public abstract boolean add(E a1);
  public abstract boolean remove(@Nullable Object a1);
  @Pure public abstract boolean containsAll(Collection<?> a1);
  public abstract boolean addAll(Collection<? extends E> a1);
  public abstract boolean retainAll(Collection<?> a1);
  public abstract boolean removeAll(Collection<?> a1);
  public abstract void clear();
  @Pure public abstract boolean equals(@Nullable Object a1);
  @Pure public abstract int hashCode();
}
