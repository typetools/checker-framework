package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to contain nonnull references.
public abstract class Dictionary<K extends /*@NonNull*/ Object, V extends /*@NonNull*/ Object> {
  public Dictionary() { throw new RuntimeException("skeleton method"); }
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract java.util.Enumeration<K> keys();
  public abstract java.util.Enumeration<V> elements();
  public abstract @Nullable V get(@Nullable java.lang.Object a1);
  public abstract @Nullable V put(K a1, V a2);
  public abstract @Nullable V remove(java.lang.Object a1);
}
