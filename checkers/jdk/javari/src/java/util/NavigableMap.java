package java.util;
import checkers.javari.quals.*;

public interface NavigableMap<K, V> extends SortedMap<K, V> {
  public abstract @PolyRead Map.Entry<K, V> lowerEntry(@PolyRead NavigableMap<K, V> this, K a1);
  public abstract K lowerKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @PolyRead Map.Entry<K, V> floorEntry(@PolyRead NavigableMap<K, V> this, K a1);
  public abstract K floorKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @PolyRead Map.Entry<K, V> ceilingEntry(@PolyRead NavigableMap<K, V> this, K a1);
  public abstract K ceilingKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @PolyRead Map.Entry<K, V> higherEntry(@PolyRead NavigableMap<K, V> this, K a1);
  public abstract K higherKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @PolyRead Map.Entry<K, V> firstEntry(@PolyRead NavigableMap<K, V> this);
  public abstract @PolyRead Map.Entry<K, V> lastEntry(@PolyRead NavigableMap<K, V> this);
  public abstract Map.Entry<K, V> pollFirstEntry();
  public abstract Map.Entry<K, V> pollLastEntry();
  public abstract @PolyRead NavigableMap<K, V> descendingMap(@PolyRead NavigableMap<K, V> this);
  public abstract @PolyRead NavigableSet<K> navigableKeySet(@PolyRead NavigableMap<K, V> this);
  public abstract @PolyRead NavigableSet<K> descendingKeySet(@PolyRead NavigableMap<K, V> this);
  public abstract @PolyRead NavigableMap<K, V> subMap(@PolyRead NavigableMap<K, V> this, K a1, boolean a2, K a3, boolean a4);
  public abstract @PolyRead NavigableMap<K, V> headMap(@PolyRead NavigableMap<K, V> this, K a1, boolean a2);
  public abstract @PolyRead NavigableMap<K, V> tailMap(@PolyRead NavigableMap<K, V> this, K a1, boolean a2);
  public abstract @PolyRead SortedMap<K, V> subMap(@PolyRead NavigableMap<K, V> this, K a1, K a2);
  public abstract @PolyRead SortedMap<K, V> headMap(@PolyRead NavigableMap<K, V> this, K a1);
  public abstract @PolyRead SortedMap<K, V> tailMap(@PolyRead NavigableMap<K, V> this, K a1);
}
