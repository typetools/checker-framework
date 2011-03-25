package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// This permits null element when using a custom comparator which allows null
public class TreeMap<K extends @Nullable Object, V extends @Nullable Object> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public TreeMap() { throw new RuntimeException("skeleton method"); }
  public TreeMap(Comparator<? super K> a1) { throw new RuntimeException("skeleton method"); }
  public TreeMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public TreeMap(SortedMap<K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public @Pure boolean containsKey(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Pure @Nullable V get(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public Comparator<? super K> comparator() { throw new RuntimeException("skeleton method"); }
  public K firstKey() { throw new RuntimeException("skeleton method"); }
  public K lastKey() { throw new RuntimeException("skeleton method"); }
  public void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> firstEntry() { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> lastEntry() { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> pollFirstEntry() { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> pollLastEntry() { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> lowerEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable K lowerKey(K a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> floorEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable K floorKey(K a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> ceilingEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable K ceilingKey(K a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable Map.Entry<K, V> higherEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable K higherKey(K a1) { throw new RuntimeException("skeleton method"); }
  public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  public NavigableSet<@KeyFor("this") K> navigableKeySet() { throw new RuntimeException("skeleton method"); }
  public NavigableSet<@KeyFor("this") K> descendingKeySet() { throw new RuntimeException("skeleton method"); }
  public Collection<V> values() { throw new RuntimeException("skeleton method"); }
  public Set<Map.Entry<@KeyFor("this") K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
  public NavigableMap<K, V> descendingMap() { throw new RuntimeException("skeleton method"); }
  public NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4) { throw new RuntimeException("skeleton method"); }
  public NavigableMap<K, V> headMap(K a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public NavigableMap<K, V> tailMap(K a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public SortedMap<K, V> subMap(K a1, K a2) { throw new RuntimeException("skeleton method"); }
  public SortedMap<K, V> headMap(K a1) { throw new RuntimeException("skeleton method"); }
  public SortedMap<K, V> tailMap(K a1) { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }

  @AssertNonNullIfFalse({"firstEntry()", "pollFirstEntry()", "lastEntry()", "pollLastEntry()"})
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
}
