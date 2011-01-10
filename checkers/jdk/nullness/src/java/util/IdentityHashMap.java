package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// This class allows null elements
public class IdentityHashMap<K extends @Nullable Object, V extends @Nullable Object> extends AbstractMap<K, V> implements Map<K, V>, java.io.Serializable, Cloneable {
  private static final long serialVersionUID = 0;
  public IdentityHashMap() { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public @Pure V get(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Pure boolean containsKey(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  public Collection<V> values() { throw new RuntimeException("skeleton method"); }
  public Set<Map.Entry<@KeyFor("this") K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
