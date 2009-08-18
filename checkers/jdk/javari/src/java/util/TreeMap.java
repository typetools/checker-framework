package java.util;
import checkers.javari.quals.*;

public class TreeMap<K, V> extends java.util.AbstractMap<K, V> implements java.util.NavigableMap<K, V>, java.lang.Cloneable, java.io.Serializable {
  public TreeMap() { throw new RuntimeException(("skeleton method")); }
  public TreeMap(java.util.Comparator<? super K> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeMap(@PolyRead java.util.Map<? extends K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public TreeMap(@PolyRead java.util.SortedMap<K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public java.util.Comparator<? super K> comparator() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public K firstKey() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public K lastKey() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Map.Entry<K, V> firstEntry() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Map.Entry<K, V> lastEntry() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public java.util.Map.Entry<K, V> pollFirstEntry() { throw new RuntimeException(("skeleton method")); }
  public java.util.Map.Entry<K, V> pollLastEntry() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Map.Entry<K, V> lowerEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K lowerKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Map.Entry<K, V> floorEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K floorKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Map.Entry<K, V> ceilingEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K ceilingKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Map.Entry<K, V> higherEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K higherKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public java.util.Set<K> keySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableSet<K> navigableKeySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableSet<K> descendingKeySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Collection<V> values() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Set<java.util.Map.Entry<K, V>> entrySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableMap<K, V> descendingMap() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableMap<K, V> headMap(K a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.NavigableMap<K, V> tailMap(K a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.SortedMap<K, V> subMap(K a1, K a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.SortedMap<K, V> headMap(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.SortedMap<K, V> tailMap(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
}
