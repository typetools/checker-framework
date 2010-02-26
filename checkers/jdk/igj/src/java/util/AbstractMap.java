package java.util;

import checkers.igj.quals.*;

@I
public abstract class AbstractMap<K, V> implements @I Map<K, V> {
  @I
  public static class SimpleEntry<K, V> implements @I Map.Entry<K, V>, @I java.io.Serializable {
      private static final long serialVersionUID = 0L;
    public SimpleEntry(K a1, V a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
    public SimpleEntry(@I Map.Entry<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
    public K getKey() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public V getValue() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  }

  @Immutable
  public static class SimpleImmutableEntry<K, V> implements @Immutable Map.Entry<K, V>, @Immutable java.io.Serializable {
      private static final long serialVersionUID = 0L;
    public SimpleImmutableEntry(K a1, V a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
    public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
    public K getKey() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public V getValue() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  }

  protected AbstractMap() @ReadOnly {}
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V get(Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V put(K a1, V a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public V remove(Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public @I Set<K> keySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Collection<V> values() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract @I Set<@I Map.Entry<K, V>> entrySet() @ReadOnly;
  public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
