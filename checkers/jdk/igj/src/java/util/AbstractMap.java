package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractMap<K, V> implements @I Map<K, V> {
  @I
  public static class SimpleEntry<K, V> implements @I Map.Entry<K, V>, @I java.io.Serializable {
      private static final long serialVersionUID = 0L;
    public SimpleEntry(@AssignsFields SimpleEntry<K, V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleEntry(@AssignsFields SimpleEntry<K, V> this, @I Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey(@ReadOnly SimpleEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
    public V getValue(@ReadOnly SimpleEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
    public V setValue(@AssignsFields SimpleEntry<K, V> this, V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly SimpleEntry<K, V> this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode(@ReadOnly SimpleEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
    public String toString(@ReadOnly SimpleEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
  }

  @Immutable
  public static class SimpleImmutableEntry<K, V> implements @Immutable Map.Entry<K, V>, @Immutable java.io.Serializable {
      private static final long serialVersionUID = 0L;
    public SimpleImmutableEntry(@AssignsFields SimpleImmutableEntry<K, V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleImmutableEntry(@AssignsFields SimpleImmutableEntry<K, V> this, Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey(@ReadOnly SimpleImmutableEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
    public V getValue(@ReadOnly SimpleImmutableEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
    public V setValue(@AssignsFields SimpleImmutableEntry<K, V> this, V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly SimpleImmutableEntry<K, V> this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode(@ReadOnly SimpleImmutableEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
    public String toString(@ReadOnly SimpleImmutableEntry<K, V> this) { throw new RuntimeException("skeleton method"); }
  }

  protected AbstractMap(@ReadOnly AbstractMap<K, V> this) {}
  public int size(@ReadOnly AbstractMap<K, V> this) { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty(@ReadOnly AbstractMap<K, V> this) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly AbstractMap<K, V> this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@ReadOnly AbstractMap<K, V> this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public V get(@ReadOnly AbstractMap<K, V> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public V put(@Mutable AbstractMap<K, V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public V remove(@Mutable AbstractMap<K, V> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void putAll(@Mutable AbstractMap<K, V> this, @ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable AbstractMap<K, V> this) { throw new RuntimeException("skeleton method"); }
  public @I Set<K> keySet(@ReadOnly AbstractMap<K, V> this) { throw new RuntimeException("skeleton method"); }
  public @I Collection<V> values(@ReadOnly AbstractMap<K, V> this) { throw new RuntimeException("skeleton method"); }
  public abstract @I Set<@I Map.Entry<K, V>> entrySet(@ReadOnly AbstractMap<K, V> this);
  public boolean equals(@ReadOnly AbstractMap<K, V> this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode(@ReadOnly AbstractMap<K, V> this) { throw new RuntimeException("skeleton method"); }
  public String toString(@ReadOnly AbstractMap<K, V> this) { throw new RuntimeException("skeleton method"); }
}
