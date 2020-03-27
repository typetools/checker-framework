package java.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {
  protected AbstractSet() {}
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(Collection<? extends @NonNull Object> a1) { throw new RuntimeException("skeleton method"); }
}
