package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

// Subclasses of this interface/class may opt to prohibit null elements.
public abstract class AbstractMap<K extends @Nullable Object, V extends @Nullable Object> implements Map<K, V> {
  protected AbstractMap() {}
  public static class SimpleEntry<K extends @Nullable Object, V extends @Nullable Object>
      implements Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleEntry(Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    @Pure public K getKey() { throw new RuntimeException("skeleton method"); }
    @Pure public V getValue() { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) { throw new RuntimeException("skeleton method"); }
    @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
    @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
    @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  }
  public static class SimpleImmutableEntry<K extends @Nullable Object, V extends @Nullable Object>
      implements Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleImmutableEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    @Pure public K getKey() { throw new RuntimeException("skeleton method"); }
    @Pure public V getValue() { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) { throw new RuntimeException("skeleton method"); }
    @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
    @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
    @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  }
  @Pure public int size() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean containsKey(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public @Nullable V get(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Collection<V> values() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public abstract Set<Map.Entry<@KeyFor("this") K, V>> entrySet();
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
}
