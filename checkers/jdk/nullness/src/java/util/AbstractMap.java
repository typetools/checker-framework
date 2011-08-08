package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements.
public abstract class AbstractMap<K extends @Nullable Object, V extends @Nullable Object> implements Map<K, V> {
  protected AbstractMap() {}
  public class SimpleEntry<K extends @Nullable Object, V extends @Nullable Object>
      implements Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleEntry(Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey() { throw new RuntimeException("skeleton method"); }
    public V getValue() { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode() { throw new RuntimeException("skeleton method"); }
    public String toString() { throw new RuntimeException("skeleton method"); }
  }
  public class SimpleImmutableEntry<K extends @Nullable Object, V extends @Nullable Object>
      implements Map.Entry<K, V>, java.io.Serializable {
    private static final long serialVersionUID = 0;
    public SimpleImmutableEntry(K a1, V a2) { throw new RuntimeException("skeleton method"); }
    public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
    public K getKey() { throw new RuntimeException("skeleton method"); }
    public V getValue() { throw new RuntimeException("skeleton method"); }
    public V setValue(V a1) { throw new RuntimeException("skeleton method"); }
    public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
    public int hashCode() { throw new RuntimeException("skeleton method"); }
    public String toString() { throw new RuntimeException("skeleton method"); }
  }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Pure boolean containsKey(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Pure V get(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  public Collection<V> values() { throw new RuntimeException("skeleton method"); }
  public abstract Set<Map.Entry<@KeyFor("this") K, V>> entrySet();
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
