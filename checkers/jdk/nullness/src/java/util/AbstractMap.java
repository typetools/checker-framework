package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractMap<K extends @NonNull Object, V extends @NonNull Object> implements java.util.Map<K, V> {
  public class SimpleEntry implements java.util.Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleEntry(java.util.Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey() { throw new RuntimeException("skeleton method"); }
    public V getValue() { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode() { throw new RuntimeException("skeleton method"); }
    public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  }
  public class SimpleImmutableEntry<K, V> implements java.util.Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleImmutableEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleImmutableEntry(java.util.Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey() { throw new RuntimeException("skeleton method"); }
    public V getValue() { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode() { throw new RuntimeException("skeleton method"); }
    public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public V get(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void putAll(java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public java.util.Set<K> keySet() { throw new RuntimeException("skeleton method"); }
  public java.util.Collection<V> values() { throw new RuntimeException("skeleton method"); }
  public abstract java.util.Set<java.util.Map.Entry<K, V>> entrySet();
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
