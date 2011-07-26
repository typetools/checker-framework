package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractSet<E extends @Nullable Object> extends AbstractCollection<E> implements Set<E> {
  protected AbstractSet() {}
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean removeAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
}
