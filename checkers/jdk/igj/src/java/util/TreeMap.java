package java.util;
import checkers.igj.quals.*;

@I
public class TreeMap<K, V> extends @I AbstractMap<K, V> implements @I NavigableMap<K, V>, @I Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeMap() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public TreeMap(@ReadOnly Comparator<? super K> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public TreeMap(@ReadOnly Map<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public TreeMap(@ReadOnly SortedMap<K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V get(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @ReadOnly Comparator<? super K> comparator() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public K firstKey() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public K lastKey() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public V put(K a1, V a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public V remove(@ReadOnly Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> firstEntry() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> lastEntry() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> pollFirstEntry() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> pollLastEntry() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> lowerEntry(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public K lowerKey(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> floorEntry(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public K floorKey(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> ceilingEntry(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public K ceilingKey(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> higherEntry(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public K higherKey(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Set<K> keySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I NavigableSet<K> navigableKeySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I NavigableSet<K> descendingKeySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Collection<V> values() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Set<@I Map.Entry<K, V>> entrySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> descendingMap() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> headMap(K a1, boolean a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> tailMap(K a1, boolean a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I SortedMap<K, V> subMap(K a1, K a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I SortedMap<K, V> headMap(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I SortedMap<K, V> tailMap(K a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
