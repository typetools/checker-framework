package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;

import java.util.stream.Stream;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Collection<E extends @Nullable Object> extends Iterable<E> {
  @Pure int size();
  @Pure boolean isEmpty();
  // not true, because map could contain nulls:  AssertParametersNonNull("get(#1)")
  @Pure boolean contains(@Nullable Object a1);
  @Override
  Iterator<E> iterator();
  // The Nullness Checker does NOT use these signatures for either version
  // of toArray; rather, the checker has hard-coded rules for those two
  // methods, because the most useful type for toArray is not expressible
  // in the surface syntax that the nullness annotations support.
  Object [] toArray();
  <T extends @Nullable Object> @Nullable T @PolyNull [] toArray(T @PolyNull [] a1);
  boolean add(E a1);
  boolean remove(@Nullable Object a1);
  @Pure public abstract boolean containsAll(Collection<?> a1);
  boolean addAll(Collection<? extends E> a1);
  boolean removeAll(Collection<?> a1);
  boolean retainAll(Collection<?> a1);
  void clear();
  @Override
  @Pure public abstract boolean equals(@Nullable Object a1);
  @Override
  @Pure public abstract int hashCode();
  default Stream<E> stream() { throw new RuntimeException("skeleton method"); }
}
