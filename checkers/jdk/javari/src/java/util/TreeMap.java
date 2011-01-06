package java.util;
import checkers.javari.quals.*;

public class TreeMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeMap() { throw new RuntimeException(("skeleton method")); }
  public TreeMap(Comparator<? super K> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeMap(@PolyRead Map<? extends K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public TreeMap(@PolyRead SortedMap<K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public Comparator<? super K> comparator() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public K firstKey() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public K lastKey() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> firstEntry() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> lastEntry() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public Map.Entry<K, V> pollFirstEntry() { throw new RuntimeException(("skeleton method")); }
  public Map.Entry<K, V> pollLastEntry() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> lowerEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K lowerKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> floorEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K floorKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> ceilingEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K ceilingKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> higherEntry(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public K higherKey(K a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public Set<K> keySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<K> navigableKeySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<K> descendingKeySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Collection<V> values() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<Map.Entry<K, V>> entrySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> descendingMap() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> headMap(K a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> tailMap(K a1, boolean a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedMap<K, V> subMap(K a1, K a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedMap<K, V> headMap(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedMap<K, V> tailMap(K a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
