package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractMap<K, V> implements @I Map<K, V> {
  @I
  public static class SimpleEntry<K, V> implements @I Map.Entry<K, V>, @I java.io.Serializable {
      private static final long serialVersionUID = 0L;
    public SimpleEntry(@AssignsFields SimpleEntry this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleEntry(@AssignsFields SimpleEntry this, @I Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
    public V getValue(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
    public V setValue(@AssignsFields SimpleEntry this, V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly SimpleEntry this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
    public String toString(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
  }

  @Immutable
  public static class SimpleImmutableEntry<K, V> implements @Immutable Map.Entry<K, V>, @Immutable java.io.Serializable {
      private static final long serialVersionUID = 0L;
    public SimpleImmutableEntry(@AssignsFields SimpleImmutableEntry this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleImmutableEntry(@AssignsFields SimpleImmutableEntry this, Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
    public V getValue(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
    public V setValue(@AssignsFields SimpleImmutableEntry this, V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly SimpleImmutableEntry this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
    public String toString(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
  }

  protected AbstractMap(@ReadOnly SimpleImmutableEntry this) {}
  public int size(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly SimpleImmutableEntry this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@ReadOnly SimpleImmutableEntry this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public V get(@ReadOnly SimpleImmutableEntry this, Object a1) { throw new RuntimeException("skeleton method"); }
  public V put(@Mutable SimpleImmutableEntry this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public V remove(@Mutable SimpleImmutableEntry this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void putAll(@Mutable SimpleImmutableEntry this, @ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
  public @I Set<K> keySet(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
  public @I Collection<V> values(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
  public abstract @I Set<@I Map.Entry<K, V>> entrySet(@ReadOnly SimpleImmutableEntry this);
  public boolean equals(@ReadOnly SimpleImmutableEntry this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
  public String toString(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
}
