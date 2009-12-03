package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractSet<E extends @NonNull Object> extends java.util.AbstractCollection<E> implements java.util.Set<E> {
  protected AbstractSet() {}
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(java.util.Collection<?> a1) { throw new RuntimeException("skeleton method"); }
}
