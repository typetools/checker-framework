package java.util;
import checkers.igj.quals.*;

@I
public class TreeMap<K, V> extends @I AbstractMap<K, V> implements @I NavigableMap<K, V>, @I Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public TreeMap(@AssignsFields TreeMap this) { throw new RuntimeException("skeleton method"); }
  public TreeMap(@AssignsFields TreeMap this, @ReadOnly Comparator<? super K> a1) { throw new RuntimeException("skeleton method"); }
  public TreeMap(@AssignsFields TreeMap this, @ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public TreeMap(@AssignsFields TreeMap this, @ReadOnly SortedMap<K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public int size(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@ReadOnly TreeMap this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly TreeMap this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public V get(@ReadOnly TreeMap this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public @ReadOnly Comparator<? super K> comparator(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public K firstKey(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public K lastKey(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public void putAll(@Mutable TreeMap this, @ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public V put(@Mutable TreeMap this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public V remove(@Mutable TreeMap this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@Mutable TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> firstEntry(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> lastEntry(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> pollFirstEntry(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> pollLastEntry(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> lowerEntry(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public K lowerKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> floorEntry(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public K floorKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> ceilingEntry(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public K ceilingKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public @I Map.Entry<K, V> higherEntry(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public K higherKey(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public @I Set<K> keySet(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I NavigableSet<K> navigableKeySet(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I NavigableSet<K> descendingKeySet(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I Collection<V> values(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I Set<@I Map.Entry<K, V>> entrySet(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> descendingMap(@ReadOnly TreeMap this) { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> subMap(@ReadOnly TreeMap this, K a1, boolean a2, K a3, boolean a4) { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> headMap(@ReadOnly TreeMap this, K a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public @I NavigableMap<K, V> tailMap(@ReadOnly TreeMap this, K a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public @I SortedMap<K, V> subMap(@ReadOnly TreeMap this, K a1, K a2) { throw new RuntimeException("skeleton method"); }
  public @I SortedMap<K, V> headMap(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public @I SortedMap<K, V> tailMap(@ReadOnly TreeMap this, K a1) { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
