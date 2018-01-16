package java.util;

import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public interface NavigableMap<K extends Object, V extends Object> extends SortedMap<K, V> {
  public abstract Map. Entry<K, V> lowerEntry(K a1);
  public abstract K lowerKey(K a1);
  public abstract Map. Entry<K, V> floorEntry(K a1);
  public abstract K floorKey(K a1);
  public abstract Map. Entry<K, V> ceilingEntry(K a1);
  public abstract K ceilingKey(K a1);
  public abstract Map. Entry<K, V> higherEntry(K a1);
  public abstract K higherKey(K a1);
  public abstract Map. Entry<K, V> firstEntry();
  public abstract Map. Entry<K, V> lastEntry();
  public abstract Map. Entry<K, V> pollFirstEntry(@GuardSatisfied NavigableMap<K, V> this);
  public abstract Map. Entry<K, V> pollLastEntry(@GuardSatisfied NavigableMap<K, V> this);
  public abstract NavigableMap<K, V> descendingMap();
  public abstract NavigableSet<K> navigableKeySet();
  public abstract NavigableSet<K> descendingKeySet();
  public abstract NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4);
  public abstract NavigableMap<K, V> headMap(K a1, boolean a2);
  public abstract NavigableMap<K, V> tailMap(K a1, boolean a2);
  public abstract SortedMap<K, V> subMap(K a1, K a2);
  public abstract SortedMap<K, V> headMap(K a1);
  public abstract SortedMap<K, V> tailMap(K a1);


   public abstract boolean isEmpty(@GuardSatisfied NavigableMap<K,V> this);
}
