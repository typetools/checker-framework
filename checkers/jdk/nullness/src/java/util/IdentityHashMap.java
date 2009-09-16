package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// This class allows null elements
public class IdentityHashMap<K extends @Nullable Object, V extends @Nullable Object> extends java.util.AbstractMap<K, V> implements java.util.Map<K, V>, java.io.Serializable, java.lang.Cloneable {
  private static final long serialVersionUID = 0;
  public IdentityHashMap() { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public V get(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public void putAll(java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public java.util.Set<K> keySet() { throw new RuntimeException("skeleton method"); }
  public java.util.Collection<V> values() { throw new RuntimeException("skeleton method"); }
  public java.util.Set<java.util.Map.Entry<K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
}
