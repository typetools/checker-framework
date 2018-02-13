package java.util;

import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements.
public abstract class AbstractMap<K extends Object, V extends Object> implements Map<K, V> {
  protected AbstractMap() {}
  public static class SimpleEntry<K extends Object, V extends Object>
      implements Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleEntry(Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
     public K getKey(@GuardSatisfied SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
     public V getValue(@GuardSatisfied SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
    public V setValue(@GuardSatisfied SimpleEntry<K,V> this, V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@GuardSatisfied SimpleEntry<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
     public int hashCode(@GuardSatisfied SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
     public String toString(@GuardSatisfied SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
  }
  public static class SimpleImmutableEntry<K extends Object, V extends Object>
      implements Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleImmutableEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
     public K getKey(@GuardSatisfied SimpleImmutableEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
     public V getValue(@GuardSatisfied SimpleImmutableEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
    public V setValue(@GuardSatisfied SimpleImmutableEntry<K,V> this, V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@GuardSatisfied SimpleImmutableEntry<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
     public int hashCode(@GuardSatisfied SimpleImmutableEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
     public String toString(@GuardSatisfied SimpleImmutableEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
  }
   public int size(@GuardSatisfied AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public boolean isEmpty(@GuardSatisfied AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@GuardSatisfied AbstractMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@GuardSatisfied AbstractMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public V get(@GuardSatisfied AbstractMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  @ReleasesNoLocks public V put(@GuardSatisfied AbstractMap<K,V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public V remove(@GuardSatisfied AbstractMap<K,V> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void putAll(@GuardSatisfied AbstractMap<K,V> this, Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<K> keySet(@GuardSatisfied AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Collection<V> values(@GuardSatisfied AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public abstract Set<Map.Entry<K,V>> entrySet(@GuardSatisfied AbstractMap<K,V> this);
  public boolean equals(@GuardSatisfied AbstractMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
}
