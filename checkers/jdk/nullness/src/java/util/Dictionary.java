package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class Dictionary<K extends @Nullable Object, V extends @Nullable Object> {
  public Dictionary() { throw new RuntimeException("skeleton method"); }
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract Enumeration<K> keys();
  public abstract Enumeration<V> elements();
  public abstract @Pure @Nullable V get(@Nullable Object a1);
  public abstract @Nullable V put(K a1, V a2);
  public abstract @Nullable V remove(Object a1);
}
