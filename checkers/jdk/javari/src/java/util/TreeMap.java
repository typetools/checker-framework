package java.util;
import checkers.javari.quals.*;

public class TreeMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeMap() { throw new RuntimeException(("skeleton method")); }
  public TreeMap(Comparator<? super K> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeMap(@PolyRead TreeMap this, @PolyRead Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public TreeMap(@PolyRead TreeMap this, @PolyRead SortedMap<K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public int size(@ReadOnly TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly TreeMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly TreeMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly TreeMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public Comparator<? super K> comparator(@ReadOnly TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public K firstKey(@ReadOnly TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public K lastKey(@ReadOnly TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly TreeMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> firstEntry(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> lastEntry(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public Map.Entry<K, V> pollFirstEntry() { throw new RuntimeException(("skeleton method")); }
  public Map.Entry<K, V> pollLastEntry() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> lowerEntry(@PolyRead TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public K lowerKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> floorEntry(@PolyRead TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public K floorKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> ceilingEntry(@PolyRead TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public K ceilingKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Map.Entry<K, V> higherEntry(@PolyRead TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public K higherKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public Set<K> keySet(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<K> navigableKeySet(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableSet<K> descendingKeySet(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Collection<V> values(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<Map.Entry<K, V>> entrySet(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> descendingMap(@PolyRead TreeMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> subMap(@PolyRead TreeMap this, K a1, boolean a2, K a3, boolean a4) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> headMap(@PolyRead TreeMap this, K a1, boolean a2) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead NavigableMap<K, V> tailMap(@PolyRead TreeMap this, K a1, boolean a2) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedMap<K, V> subMap(@PolyRead TreeMap this, K a1, K a2) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedMap<K, V> headMap(@PolyRead TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead SortedMap<K, V> tailMap(@PolyRead TreeMap this, K a1) { throw new RuntimeException(("skeleton method")); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
