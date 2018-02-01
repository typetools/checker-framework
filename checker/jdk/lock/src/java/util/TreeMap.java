package java.util;



import org.checkerframework.checker.lock.qual.*;

// This permits null element when using a custom comparator which allows null
public class TreeMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public TreeMap() { throw new RuntimeException("skeleton method"); }
  public TreeMap(Comparator<? super K> a1) { throw new RuntimeException("skeleton method"); }
  public TreeMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public TreeMap(SortedMap<K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@GuardSatisfied TreeMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@GuardSatisfied TreeMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public V get(@GuardSatisfied TreeMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public Comparator<? super K> comparator(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public K firstKey() { throw new RuntimeException("skeleton method"); }
  public K lastKey() { throw new RuntimeException("skeleton method"); }
  public void putAll(@GuardSatisfied TreeMap<K,V> this, Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public V put(@GuardSatisfied TreeMap<K,V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public V remove(@GuardSatisfied TreeMap<K,V> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> firstEntry() { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> lastEntry() { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> pollFirstEntry(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> pollLastEntry(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> lowerEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public K lowerKey(K a1) { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> floorEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public K floorKey(K a1) { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> ceilingEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public K ceilingKey(K a1) { throw new RuntimeException("skeleton method"); }
  public Map. Entry<K, V> higherEntry(K a1) { throw new RuntimeException("skeleton method"); }
  public K higherKey(K a1) { throw new RuntimeException("skeleton method"); }
   public Set<K> keySet(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public NavigableSet<K> navigableKeySet(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public NavigableSet<K> descendingKeySet(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Collection<V> values(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<Map.Entry<K,V>> entrySet(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public NavigableMap<K, V> descendingMap(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public NavigableMap<K, V> subMap(@GuardSatisfied TreeMap<K,V> this, @GuardSatisfied K a1, boolean a2, @GuardSatisfied K a3, boolean a4) { throw new RuntimeException("skeleton method"); }
  public NavigableMap<K, V> headMap(@GuardSatisfied TreeMap<K,V> this, @GuardSatisfied K a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public NavigableMap<K, V> tailMap(@GuardSatisfied TreeMap<K,V> this, @GuardSatisfied K a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public SortedMap<K, V> subMap(@GuardSatisfied TreeMap<K,V> this, @GuardSatisfied K a1, @GuardSatisfied K a2) { throw new RuntimeException("skeleton method"); }
  public SortedMap<K, V> headMap(@GuardSatisfied TreeMap<K,V> this, K a1) { throw new RuntimeException("skeleton method"); }
  public SortedMap<K, V> tailMap(@GuardSatisfied TreeMap<K,V> this, K a1) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }


   public boolean isEmpty(@GuardSatisfied TreeMap<K,V> this) { throw new RuntimeException("skeleton method"); }
}
